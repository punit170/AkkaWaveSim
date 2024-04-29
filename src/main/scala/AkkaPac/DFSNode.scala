package AkkaPac

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import scala.util.Random
import scala.collection.Set
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/*
* Rules:
* 1. A process never forwards the token through the same channel in the same direction twice (channels A->B and B->A are different)
* 2. A process only forwards the token to its parent if there is no other option
* 3. When a process receives the token, it immediately sends it back through the same channel if it is allowed
     by rule 1 and 2
*/

object DFSNode extends commonWaveActor {
  //message types
  
  type DFSMessage = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[DFSMessage] =
    Behaviors.setup { context =>
      new DFSNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[DFSMessage]], ListBuffer.empty[ActorRef[DFSMessage]], -1)
    }
}

class DFSNode(context: ActorContext[DFSNode.DFSMessage], spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import DFSNode._

  //states: idle, active, passive
  private def idle(neighbours: ListBuffer[ActorRef[DFSMessage]],
                   neighboursVisited: ListBuffer[ActorRef[DFSMessage]],
                   actorId: Int): Behavior[DFSMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        if (neighbours.isEmpty) {
          context.log.info("No neighbour for initial actor!")
          passive()
        }

        val sendToIndex = Random.nextInt(neighbours.length)
        val sendTo = neighbours(sendToIndex)

        neighbours -= sendTo
        neighboursVisited.addOne(sendTo)

        context.log.info(s"Initiator Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
        sendTo ! SendTokenToNeighbour(context.self)

        active(null, neighbours, neighboursVisited, actorId)

      case m2: SendTokenToNeighbour =>

        context.log.info(s"Actor${context.self.path.name} receives token for first time and makes Actor${m2.sender.path.name} its parent")

        val parent = m2.sender
        neighbours -= parent

        if (neighbours.nonEmpty) {
          val sendToIndex = Random.nextInt(neighbours.length)
          val sendTo = neighbours(sendToIndex)
          neighbours -= sendTo
          neighboursVisited.addOne(sendTo)

          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
          sendTo ! SendTokenToNeighbour(context.self)

          active(parent, neighbours, neighboursVisited, actorId)
        }
        else /*if (parent != null)*/ {
          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name} (only neighbour)")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"Actor${context.self.path.name} received updateNeighbours() from MainActor. Neighbours updated: ${neighbours.map(n => "Actor" + n.path.name)}!")
        m3.sender ! WaveMain.setNeighbourAckForDFSAlgo(context.self)
        idle(neighbours, neighboursVisited, actorId)

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"Actor${context.self.path.name} received setActorID() from MainActor. Actor id set to: ${m5.actorId}!")
        m5.sender ! WaveMain.setIdAckForDFSAlgo(context.self)
        idle(neighbours, neighboursVisited, m5.actorId)

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
        
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled
    }
  }

  private def active(parent: ActorRef[DFSMessage],
                      neighbours: ListBuffer[ActorRef[DFSMessage]],
                      neighboursVisited: ListBuffer[ActorRef[DFSMessage]],
                      actorId: Int): Behavior[DFSMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        context.log.warn("Warning! Active process received InitializeTraversal message. This behavior is unhandled.")
        Behaviors.unhandled

      case m2: SendTokenToNeighbour =>
        if (neighbours.nonEmpty) {
          val sendTo = {
            if (neighbours.contains(m2.sender)) {
              context.log.info(s"ACTIVE: Actor${context.self.path.name} is immediately sending back token to Actor${m2.sender.path.name}")
              m2.sender
            }
            else {
              val sendToIndex = Random.nextInt(neighbours.length)
              context.log.info(s"ACTIVE: Actor${context.self.path.name} is sending the token to Actor${neighbours(sendToIndex).path.name}")
              neighbours(sendToIndex)
            }
          }
          neighbours -= sendTo
          neighboursVisited.addOne(sendTo)

          sendTo ! SendTokenToNeighbour(context.self)

          active(parent, neighbours, neighboursVisited, actorId)
        }
        else if (neighbours.isEmpty && parent != null) {
          context.log.info(s"ACTIVE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }
        else {
          context.log.info("Traversal ends!!")
          spawnerActor ! WaveMain.shutSystemForDFSAlgo(context.self)
          Behaviors.stopped
        }

      case _: updateNeighbours =>
        context.log.warn("Warning! Active process received updateNeighbour message. This behavior is unhandled.")
        Behaviors.unhandled

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case _: setActorID =>
        context.log.warn("Warning! Active process received setActorID message. This behavior is unhandled.")
        Behaviors.unhandled

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
        
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled  
    }
  }

  private def passive(): Behavior[DFSMessage] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }
}