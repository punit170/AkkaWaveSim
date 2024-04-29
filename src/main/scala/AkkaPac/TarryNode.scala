package AkkaPac

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext

import scala.util.Random
import scala.collection.Set
import scala.collection.mutable.ListBuffer


object TarryNode extends commonWaveActor {

  //message types
  type Message = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[Message] =
    Behaviors.setup { context =>
      new TarryNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[Message]], ListBuffer.empty[ActorRef[Message]], -1)
    }
}

class TarryNode(context: ActorContext[TarryNode.Message], spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import TarryNode._

  //states: idle, active, passive
  private def idle(neighbours: ListBuffer[ActorRef[Message]],
                    neighboursVisited: ListBuffer[ActorRef[Message]],
                    actorId: Int): Behavior[Message] = {
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

        context.log.info("")
        context.log.info(s"Initiator Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
        sendTo ! SendTokenToNeighbour(context.self)

        active(null, neighbours, neighboursVisited, actorId)

      case m2: SendTokenToNeighbour =>
        context.log.info(s"Actor${context.self.path.name} receives token for first time and makes Actor${m2.sender.path.name} its parent")

        val parent = m2.sender
        assert(neighbours.contains(parent))
        neighbours -= parent

        if (neighbours.nonEmpty) {
          val sendToIndex: Int = Random.nextInt(neighbours.length)
          val sendTo = neighbours(sendToIndex)
          neighbours -= sendTo
          neighboursVisited.addOne(sendTo)

          context.log.info(s"Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
          sendTo ! SendTokenToNeighbour(context.self)

          active(parent, neighbours, neighboursVisited, actorId)
        }
        else /*if (parent != null)*/ {
          context.log.info(s"Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"Actor${context.self.path.name} received updateNeighbours() from MainActor. Neighbours updated: ${neighbours.map(n => "Actor" + n.path.name)}!")
        m3.sender ! WaveMain.setNeighbourAckForTarryAlgo(context.self)
        idle(neighbours, neighboursVisited, actorId)

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"Actor${context.self.path.name} received setActorID() from MainActor. Actor id set to: ${m5.actorId}!")
        m5.sender ! WaveMain.setIdAckForTarryAlgo(context.self)
        idle(neighbours, neighboursVisited, m5.actorId)

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled
    }
  }

  private def active(parent: ActorRef[Message],
                     neighbours: ListBuffer[ActorRef[Message]],
                     neighboursVisited: ListBuffer[ActorRef[Message]],
                     actorId: Int): Behavior[Message] = 
    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        context.log.warn("Warning! Active process received InitializeTraversal message. This behavior is unhandled.")
        Behaviors.unhandled
  
      case _: SendTokenToNeighbour =>
        if (neighbours.nonEmpty) {
          val sendToIndex = Random.nextInt(neighbours.length)
          val sendTo = neighbours(sendToIndex)
          neighbours -= sendTo
          neighboursVisited.addOne(sendTo)
  
          context.log.info(s"Actor${context.self.path.name} is sending token to Actor${sendTo.path.name}")
          sendTo ! SendTokenToNeighbour(context.self)
  
          active(parent, neighbours, neighboursVisited, actorId)
        }
        else if (neighbours.isEmpty && parent != null) {
          context.log.info(s"Actor${context.self.path.name} is sending token back to parent Actor${parent.path.name}")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }
        else {
          context.log.info("Traversal ends!!\n")
          spawnerActor ! WaveMain.shutSystemForTarryAlgo(context.self)
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

  private def passive(): Behavior[Message] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }
}

