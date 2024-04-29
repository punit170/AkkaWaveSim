package AkkaPac

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import scala.util.Random
import scala.collection.Set
import scala.collection.mutable.ListBuffer

/*
* Rules:
In addition to DFS, send visited neighbours list so far
*/

object OptimizedDFSNode extends commonWaveActor {
  //message types
  final case class SendTokenToNeighbour_OptimDFS(sender: ActorRef[OptimizedDFSNodeMessage], visitedNodes: List[ActorRef[OptimizedDFSNodeMessage]]) extends TokenMessages

  type OptimizedDFSNodeMessage = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[OptimizedDFSNodeMessage] =
    Behaviors.setup { context =>
      new OptimizedDFSNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[OptimizedDFSNodeMessage]], -1)
    }
}

class OptimizedDFSNode(context: ActorContext[OptimizedDFSNode.OptimizedDFSNodeMessage], spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import OptimizedDFSNode._

  //states: idle, active, passive
  private def idle(neighbours: ListBuffer[ActorRef[OptimizedDFSNodeMessage]],
                    actorId: Int): Behavior[OptimizedDFSNodeMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        if (neighbours.isEmpty) {
          context.log.info("No neighbour for initial actor!")
          passive()
        }

        val sendToIndex = Random.nextInt(neighbours.length)
        val sendTo = neighbours(sendToIndex)

        neighbours -= sendTo

        val visitedList = List(context.self)
        context.log.info(s"Initiator Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
        sendTo ! SendTokenToNeighbour_OptimDFS(context.self, visitedList)

        active(null, neighbours, actorId)

      case m2: SendTokenToNeighbour_OptimDFS =>

        context.log.info(s"Actor${context.self.path.name} receives token for first time and makes Actor${m2.sender.path.name} its parent")

        val parent = m2.sender
        neighbours -= parent

        val updatedVisitedList = m2.visitedNodes ++ List(context.self)

        //update the neighbors list
        updateNeighboursList(neighbours, m2.visitedNodes, actorId)

        if (neighbours.nonEmpty) {
          val sendToIndex = Random.nextInt(neighbours.length)
          val sendTo = neighbours(sendToIndex)
          neighbours -= sendTo

          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
          sendTo ! SendTokenToNeighbour_OptimDFS(context.self, updatedVisitedList)

          active(parent, neighbours, actorId)
        }
        else {
          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name} (only neighbour)")
          parent ! SendTokenToNeighbour_OptimDFS(context.self, updatedVisitedList)
          passive()
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"Actor${context.self.path.name} received updateNeighbours() from MainActor. Neighbours updated: ${neighbours.map(n => "Actor" + n.path.name)}!")
        m3.sender ! WaveMain.setNeighbourAckForOptimizedDFSAlgo(context.self)
        idle(neighbours, actorId)

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"Actor${context.self.path.name} received setActorID() from MainActor. Actor id set to: ${m5.actorId}!")
        m5.sender ! WaveMain.setIdAckForOptimizedDFSAlgo(context.self)
        idle(neighbours, m5.actorId)

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
        
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled
    }
  }

  private def active(
                      parent: ActorRef[OptimizedDFSNodeMessage],
                      neighbours: ListBuffer[ActorRef[OptimizedDFSNodeMessage]],
                      actorId: Int
                    ): Behavior[OptimizedDFSNodeMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        context.log.warn("Warning! Active process received InitializeTraversal message. This behavior is unhandled.")
        Behaviors.unhandled

      case m2: SendTokenToNeighbour_OptimDFS =>

        //update the neighbors list
        updateNeighboursList(neighbours, m2.visitedNodes, actorId)

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

          sendTo ! SendTokenToNeighbour_OptimDFS(context.self, m2.visitedNodes)

          active(parent, neighbours, actorId)
        }
        else if (neighbours.isEmpty && parent != null) {
          context.log.info(s"ACTIVE: Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")
          parent ! SendTokenToNeighbour_OptimDFS(context.self, m2.visitedNodes)
          passive()
        }
        else {
          context.log.info("Traversal ends!!")
          spawnerActor ! WaveMain.shutSystemForOptimizedDFSAlgo(context.self)
          Behaviors.stopped
        }

      case _: updateNeighbours =>
        context.log.warn("Warning! Active process received updateNeighbour message. This behavior is unhandled.")
        Behaviors.unhandled

      case m4: getNeighbours =>
        context.log.info(s"${m4.actorId} -> neighbours: $neighbours")
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

  private def passive(): Behavior[OptimizedDFSNodeMessage] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }

  private def updateNeighboursList(neighbours: ListBuffer[ActorRef[OptimizedDFSNodeMessage]],
                               visitedNodes: List[ActorRef[OptimizedDFSNodeMessage]],
                               actorId: Int): Unit = {
    val neighboursAlreadyVisited = neighbours.intersect(visitedNodes)
    if (neighboursAlreadyVisited.nonEmpty) {
      context.log.info(s"Actor$actorId- Neighbours already visited: $neighboursAlreadyVisited")
      neighboursAlreadyVisited.foreach(actRef => neighbours -= actRef)
    }
  }

}