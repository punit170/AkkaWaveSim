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

object AwerbuchDFSNode extends commonWaveActor {
  //Info message types
  final private case class InfoMessage(sender: ActorRef[AwerbuchDFSNodeMessage]) extends Information
  final private case class InfoAck(sender: ActorRef[AwerbuchDFSNodeMessage]) extends Information

  type AwerbuchDFSNodeMessage = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[AwerbuchDFSNodeMessage] =
    Behaviors.setup { context =>
      new AwerbuchDFSNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[AwerbuchDFSNodeMessage]], -1, ListBuffer.empty[ActorRef[AwerbuchDFSNodeMessage]],ListBuffer.empty[ActorRef[AwerbuchDFSNodeMessage]],ListBuffer.empty[ActorRef[AwerbuchDFSNodeMessage]])
    }
}


class AwerbuchDFSNode(context: ActorContext[AwerbuchDFSNode.AwerbuchDFSNodeMessage],spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import AwerbuchDFSNode.*

  //states: idle, intermediate, active
  private def idle(neighbours: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                   actorId: Int,
                   InfoMessageSentTo: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                   InfoMessageReceivedFrom: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                   frondEdgeNeighbours: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]]
                  ): Behavior[AwerbuchDFSNodeMessage] = {
    
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

        //update list with neighbours to be sent info message
        InfoMessageSentTo.addAll(neighbours)

        //send info messages to neighbours if they exist
        if (InfoMessageSentTo.nonEmpty) {
          context.log.info(s"IDLE: Initiator Actor${context.self.path.name} is sending InfoMessages to ${neighbours.map(n => "Actor" + n.path.name)}")
          neighbours.foreach(n => n ! InfoMessage(context.self))
          //actor goes into an intermediate state, where it waits for all the acknowledgements before forwarding the token and going into the active state
          intermediate(null, neighbours, actorId, InfoMessageSentTo, InfoMessageReceivedFrom, frondEdgeNeighbours, sendTo)
        }
        //if only one neighbour, send token to the decided neighbour
        else {
          context.log.info(s"Initiator Actor${context.self.path.name} is sending token to ${sendTo.path.name}")
          sendTo ! SendTokenToNeighbour(context.self)
          active(null, neighbours, actorId, InfoMessageReceivedFrom, frondEdgeNeighbours)
        }

      case m2: SendTokenToNeighbour =>

        context.log.info(s"IDLE: Actor${context.self.path.name} receives token for first time and makes Actor${m2.sender.path.name} its parent")

        //set parent, and remove it from neighbours
        val parent = m2.sender
        neighbours -= parent

        //update the frond edges
        if (InfoMessageReceivedFrom.contains(parent)) {
          InfoMessageReceivedFrom -= parent
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
              context.log.info(s"IDLE: Actor${context.self.path.name} decides to send token next to ${anEligibleNeighbour.path.name}")
              neighboursEligibleForToken -= anEligibleNeighbour
              neighbours -= anEligibleNeighbour
              anEligibleNeighbour
            }
            else {
              parent
            }
          }

          //send Info message to all neighbours except parent/next to be token holder
          InfoMessageSentTo.addAll(neighbours)
          if (InfoMessageSentTo.nonEmpty) {
            context.log.info(s"IDLE: Actor${context.self.path.name} is sending InfoMessages to ${neighbours.map(n => "Actor" + n.path.name)}")
            neighbours.foreach(n => n ! InfoMessage(context.self))
            intermediate(parent, neighboursEligibleForToken, actorId, InfoMessageSentTo, InfoMessageReceivedFrom, frondEdgeNeighbours, sendTo)
          }
          //if only one non-frond neighbour, send it the token
          else if (sendTo != parent) {
            context.log.info(s"IDLE: Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
            sendTo ! SendTokenToNeighbour(context.self)
            active(parent, neighboursEligibleForToken, actorId, InfoMessageReceivedFrom, frondEdgeNeighbours)

          }
          //if no non-frond neighbours exists, simply send token back to the parent
          else {
            context.log.info(s"IDLE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")
            parent ! SendTokenToNeighbour(context.self)
            passive()
          }
        }
        //if no neighbours exists, simply send token back to the parent
        else {
          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name} (only neighbour)")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"${context.self.path.name} received updateNeighbours() from ${m3.sender}. Neighbours updated: $neighbours!")
        m3.sender ! WaveMain.setNeighbourAckForAwerbuchDFSAlgo(context.self)
        idle(neighbours, actorId, InfoMessageSentTo, InfoMessageReceivedFrom, frondEdgeNeighbours)

      case m4: getNeighbours =>
        context.log.info(s"${m4.actorId} -> neighbours: $neighbours")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"${context.self.path.name} received setActorID() from ${m5.sender}. Actor id set to: $actorId!")
        m5.sender ! WaveMain.setIdAckForAwerbuchDFSAlgo(context.self)
        idle(neighbours, m5.actorId, InfoMessageSentTo, InfoMessageReceivedFrom, frondEdgeNeighbours)

      case m6: InfoMessage =>
        //Store nodes that send InfoMessages- used to detect frond edge nodes later
        InfoMessageReceivedFrom += m6.sender
        context.log.info(s"IDLE: Actor${context.self.path.name} received InfoMessage from Actor${m6.sender.path.name}. It's sending back an ack.")
        //send back acknowledgement
        m6.sender ! InfoAck(context.self)
        Behaviors.same

      //only current token holder in intermediate state can receive acknowledgement messages
      case _: InfoAck =>
        context.log.warn("Warning! Idle process received InfoAck message. This behavior is unhandled.")
        Behaviors.unhandled

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
        
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled
    }
  }

  private def intermediate( parent: ActorRef[AwerbuchDFSNodeMessage],
                            neighboursEligibleForToken: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                            actorId: Int,
                            InfoMessageSentTo: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                            InfoMessageReceivedFrom: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                            frondEdgeNeighbours: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                            sendTo: ActorRef[AwerbuchDFSNodeMessage]
                          ): Behavior[AwerbuchDFSNodeMessage] = {

    Behaviors.receiveMessage {

      case m7: InfoAck =>
        context.log.info(s"Intermediate: Actor${context.self.path.name} received InfoAck from Actor${m7.sender.path.name}.")

        //update nodes that sent back acknowledgements
        InfoMessageSentTo -= m7.sender
        if (InfoMessageSentTo.nonEmpty) {
          Behaviors.same
        }
        //forward token once all acknowledgements received
        else {
          context.log.info(s"Intermediate: Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}.")
          sendTo ! SendTokenToNeighbour(context.self)
          active(parent, neighboursEligibleForToken, actorId, InfoMessageReceivedFrom, frondEdgeNeighbours)
        }

      case message => context.log.warn(s"Warning! Intermediate process received ${message.getClass()} message. This behavior is unhandled.")
        Behaviors.unhandled
    }

  }

  private def active(
                      parent: ActorRef[AwerbuchDFSNodeMessage],
                      neighbours: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                      actorId: Int,
                      InfoMessageReceivedFrom: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]],
                      frondEdgeNeighbours: ListBuffer[ActorRef[AwerbuchDFSNodeMessage]]
                    ): Behavior[AwerbuchDFSNodeMessage] = {

    Behaviors.receiveMessage {

      case m2: SendTokenToNeighbour =>
        //when node sees token for the second time, add new frond nodes (- from ones that newly sent the InfoMessage)
        if (InfoMessageReceivedFrom.contains(m2.sender))
          InfoMessageReceivedFrom -= m2.sender
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
          active(parent, validNeighbours, actorId, InfoMessageReceivedFrom, frondEdgeNeighbours)
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
          spawnerActor ! WaveMain.shutSystemForAwerbuchDFSAlgo(context.self)
          Behaviors.stopped
        }

      case m6: InfoMessage =>
        //Store nodes that send InfoMessages- used to detect frond edge nodes later when this node receives the token again
        InfoMessageReceivedFrom += m6.sender
        context.log.info(s"IDLE: ${context.self} received InfoMessage from ${m6.sender}. It's sending back an ack.")
        m6.sender ! InfoAck(context.self)
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

  private def passive(): Behavior[AwerbuchDFSNodeMessage] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }
}