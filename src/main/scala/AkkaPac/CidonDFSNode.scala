package AkkaPac

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.collection.{Set, mutable}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Random, Success}
import concurrent.duration.DurationInt
/*
* Rules:
In addition to 2 rules of Tarry's algo, send info message to remaining neighbours and wait for ack.
*/

object CidonDFSNode extends commonWaveActor {
  //Info message types
  final private case class InfoMessage(sender: ActorRef[CidonDFSNodeMessage]) extends Information
//  final private case class InfoAck(sender: ActorRef[CidonDFSNodeMessage]) extends Information

  type CidonDFSNodeMessage = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[CidonDFSNodeMessage] =
    Behaviors.setup { context =>
      new CidonDFSNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[CidonDFSNodeMessage]], -1, null,ListBuffer.empty[ActorRef[CidonDFSNodeMessage]],ListBuffer.empty[ActorRef[CidonDFSNodeMessage]])
    }
}


class CidonDFSNode(context: ActorContext[CidonDFSNode.CidonDFSNodeMessage], spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import CidonDFSNode.*

  //states: idle, active
  private def idle( neighbours: ListBuffer[ActorRef[CidonDFSNodeMessage]],
                    actorId: Int,
                    lastForwarded: ActorRef[CidonDFSNodeMessage],
                    InfoMessageReceivedFrom: ListBuffer[ActorRef[CidonDFSNodeMessage]],
                    frondEdgeNeighbours: ListBuffer[ActorRef[CidonDFSNodeMessage]]
                  ): Behavior[CidonDFSNodeMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        //edge case- no neighbours for initiator
        if (neighbours.isEmpty) {
          context.log.info("No neighbour for initial actor!")
          passive()
        }

        //select neighbour to send token to next
        val sendToIndex = Random.nextInt(neighbours.length)
        val sendTo = neighbours(sendToIndex)
        //update neighbours
        neighbours -= sendTo
        context.log.info(s"IDLE: Initiator Actor${context.self.path.name} decides to send token next to ${sendTo.path.name}")
        
        context.log.info(s"IDLE: Initiator Actor${context.self} is sending token to Actor${sendTo.path.name}")
        sendTo ! SendTokenToNeighbour(context.self)

        if (neighbours.nonEmpty) {
          context.log.info(s"IDLE: Initiator Actor${context.self.path.name} is sending InfoMessages to ${neighbours.map(n => "Actor" + n.path.name)}")
          neighbours.foreach(n => n ! InfoMessage(context.self))
        }

        active(null, neighbours, actorId, sendTo, InfoMessageReceivedFrom, frondEdgeNeighbours)

      case m2: SendTokenToNeighbour =>

        context.log.info(s"IDLE: Actor${context.self.path.name} receives token for first time and makes Actor${m2.sender.path.name} its parent")

        //set parent, and remove it from neighbours
        val parent = m2.sender
        neighbours -= parent

        //update the frond edges (edges from which info message was received but not the token)
        if (InfoMessageReceivedFrom.contains(parent)) {
          InfoMessageReceivedFrom -= parent
        }

        if (InfoMessageReceivedFrom.nonEmpty) {
          context.log.info(s"IDLE: Actor${context.self.path.name} is adding frond edges: ${InfoMessageReceivedFrom.map(n => "Actor" + n.path.name)}")
        }

        //This node sees token for the first time- update frondEdgeNeighbours
        InfoMessageReceivedFrom.foreach(node => frondEdgeNeighbours += node)

        if (neighbours.nonEmpty) {
          //determine node to which token to be send to
          val neighboursEligibleForToken = neighbours.diff(frondEdgeNeighbours)

          //send to non-frond edged neighbour or else back to parent
          val sendTo = {
            if (neighboursEligibleForToken.nonEmpty) {
              val sendToIndex = Random.nextInt(neighboursEligibleForToken.length)
              val anEligibleNeighbour = neighboursEligibleForToken(sendToIndex)
              context.log.info(s"IDLE: Actor${context.self.path.name} decides to send token next to Actor${anEligibleNeighbour.path.name}")
              neighboursEligibleForToken -= anEligibleNeighbour
              neighbours -= anEligibleNeighbour
              anEligibleNeighbour
            }
            else {
              parent
            }
          }

          if (sendTo != parent) {
            context.log.info(s"IDLE: Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
            sendTo ! SendTokenToNeighbour(context.self)

            if (neighbours.nonEmpty) {
              context.log.info(s"IDLE: Actor${context.self.path.name} is sending InfoMessages to ${neighbours.map(n => "Actor" + n.path.name)}")
              neighbours.foreach(n => n ! InfoMessage(context.self))
            }

            active(parent, neighboursEligibleForToken, actorId, sendTo, InfoMessageReceivedFrom, frondEdgeNeighbours)

          }
          //if no non-frond neighbours exists, simply send token back to the parent
          else {
            context.log.info(s"IDLE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")

            if (neighbours.nonEmpty) {
              context.log.info(s"IDLE: Actor${context.self.path.name} is sending InfoMessages to ${neighbours.map(n => "Actor" + n.path.name)}")
              neighbours.foreach(n => n ! InfoMessage(context.self))
            }

            parent ! SendTokenToNeighbour(context.self)
            passive()
          }
        }
        //if no neighbours exists, simply send token back to the parent
        else {
          context.log.info(s"IDLE: Actor${context.self} is sending token back to parent Actor${parent.path.name} (only neighbour)")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"Actor${context.self.path.name} received updateNeighbours() from Actor${m3.sender}. Neighbours updated: ${neighbours.map(n => "Actor" + n.path.name)}!")
        m3.sender ! WaveMain.setNeighbourAckForCidonDFSAlgo(context.self)
        idle(neighbours, actorId, lastForwarded, InfoMessageReceivedFrom, frondEdgeNeighbours)

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"Actor${context.self.path.name} received setActorID() from Actor${m5.sender.path.name}. Actor id set to: $actorId!")
        m5.sender ! WaveMain.setIdAckForCidonDFSAlgo(context.self)
        idle(neighbours, m5.actorId, lastForwarded, InfoMessageReceivedFrom, frondEdgeNeighbours)

      case m6: InfoMessage =>
        //Store nodes that send InfoMessages- used to detect frond edge nodes later
        InfoMessageReceivedFrom += m6.sender
        context.log.info(s"IDLE: Actor${context.self.path.name} received InfoMessage from Actor${m6.sender.path.name}.")
        Behaviors.same

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled  
    }
    
  }

  private def active( parent: ActorRef[CidonDFSNodeMessage],
                      neighbours: ListBuffer[ActorRef[CidonDFSNodeMessage]],
                      actorId: Int,
                      lastForwarded: ActorRef[CidonDFSNodeMessage],
                      InfoMessageReceivedFrom: ListBuffer[ActorRef[CidonDFSNodeMessage]],
                      frondEdgeNeighbours: ListBuffer[ActorRef[CidonDFSNodeMessage]]
                    ): Behavior[CidonDFSNodeMessage] = {

    Behaviors.receiveMessage {

      case m2: SendTokenToNeighbour =>

        if (m2.sender == context.self) {
          context.log.info(s"ACTIVE: Actor${context.self.path.name} received token message from self")
        }

        if (lastForwarded == m2.sender || m2.sender == context.self) {
          //when node sees token for the second time, add new frond nodes (- from ones that newly sent the InfoMessage)
          if (lastForwarded == m2.sender && InfoMessageReceivedFrom.contains(m2.sender))
            InfoMessageReceivedFrom -= m2.sender

          if (InfoMessageReceivedFrom.diff(frondEdgeNeighbours).nonEmpty) {
            context.log.info(s"ACTIVE: Actor${context.self.path.name} is adding frond edges: ${InfoMessageReceivedFrom.diff(frondEdgeNeighbours).map(n => "Actor" + n.path.name)}")
          }

          InfoMessageReceivedFrom.diff(frondEdgeNeighbours).foreach(node => frondEdgeNeighbours += node)

          //valid neighbours i.e., non-frond nodes list
          val validNeighbours = neighbours.diff(frondEdgeNeighbours)

          if (validNeighbours.nonEmpty) {
            //determine non-frond edged node to which token to be send to next
            val sendToIndex = Random.nextInt(validNeighbours.length)
            val sendTo = validNeighbours(sendToIndex)
            validNeighbours -= sendTo

            context.log.info(s"IDLE: Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
            sendTo ! SendTokenToNeighbour(context.self)
            active(parent, validNeighbours, actorId, sendTo, InfoMessageReceivedFrom, frondEdgeNeighbours)
          }

          //if non-initiator has non non-frond edged node- send token back to parent
          else if (validNeighbours.isEmpty && parent != null) {
            context.log.info(s"ACTIVE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")
            parent ! SendTokenToNeighbour(context.self)
            passive()
          }
          //initiator receives back the token with no non-frond neighbours left
          else {
            context.log.info(s"Traversal ends!!")
            spawnerActor ! WaveMain.shutSystemForCidonDFSAlgo(context.self)
            Behaviors.stopped
          }
        }

        else {
          context.log.info(s"ACTIVE: ${context.self} received back token from unexpected node ${m2.sender} and adding it as frond edge node.")
          frondEdgeNeighbours += m2.sender
          Behaviors.same
        }

      case m6: InfoMessage =>
        //Store nodes that send InfoMessages- used to detect frond edge nodes later when this node receives the token again
        InfoMessageReceivedFrom += m6.sender
        context.log.info(s"ACTIVE: ${context.self} received InfoMessage from ${m6.sender}.")
        //m6.sender ! InfoAck(context.self)

        //info message from p received very late
        if (lastForwarded == m6.sender) {
          context.log.info(s"ACTIVE: ${context.self} received InfoMessage from lastForwarded node! .")
          neighbours += m6.sender
          context.log.info(s"ACTIVE: ${context.self} resend token message to itself! .")
          context.self ! SendTokenToNeighbour(context.self)
        }
        Behaviors.same

      case m4: getNeighbours =>
        context.log.info(s"${m4.actorId} -> neighbours: $neighbours")
        Behaviors.same

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same

      case message =>
        context.log.warn(s"Warning! Active process received ${message.getClass()} message. This behavior is unhandled.")
        Behaviors.unhandled
    }
  }

  private def passive(): Behavior[CidonDFSNodeMessage] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }
}