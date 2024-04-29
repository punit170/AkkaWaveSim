package AkkaPac

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext

import scala.util.Random
import scala.collection.Set
import scala.collection.mutable.ListBuffer


object TreeAlgoNode extends commonWaveActor {

  //message types
  type TreeAlgoMessage = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[TreeAlgoMessage] =
    Behaviors.setup { context =>
      new TreeAlgoNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[TreeAlgoMessage]] ,ListBuffer.empty[ActorRef[TreeAlgoMessage]], -1)
    }
}

class TreeAlgoNode(context: ActorContext[TreeAlgoNode.TreeAlgoMessage], spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import TreeAlgoNode._

  //states: idle, active
  private def idle( neighbours: ListBuffer[ActorRef[TreeAlgoMessage]],
                    receivedFromNeighbours: ListBuffer[ActorRef[TreeAlgoMessage]],
                    actorId: Int
                  ): Behavior[TreeAlgoMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        if (neighbours.isEmpty) {
          context.log.info(s"No neighbour for initial actor ${context.self}!")
          passive()
        }

        //only leaf nodes can send initial messages
        assert(neighbours.length == 1)

        val sendTo = neighbours.head

        context.log.info(s"IDLE: Leaf node- Actor${context.self.path.name} is sending token to: ${neighbours.map(n => "Actor" + n.path.name)}")
        sendTo ! SendTokenToNeighbour(context.self)

        active(sendTo, neighbours, actorId)

      case m2: SendTokenToNeighbour =>
        context.log.info(s"IDLE: Actor${context.self.path.name} received message from  Actor${m2.sender.path.name}")
        receivedFromNeighbours.addOne(m2.sender)

        val yetToReceiveFrom = neighbours.diff(receivedFromNeighbours)

        if (yetToReceiveFrom.length == 1) {
          val parent = yetToReceiveFrom.head
          context.log.info(s"IDLE: Actor${context.self.path.name} is sending message to parent Actor${parent.path.name}")
          parent ! SendTokenToNeighbour(context.self)
          active(parent, neighbours, actorId)
        }
        else {
          Behaviors.same
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"${context.self.path.name} received updateNeighbours() from Actor${m3.sender.path.name}. Neighbours updated: ${neighbours.map(n => "Actor" + n.path.name)}!")
        m3.sender ! WaveMain.setNeighbourAckForTreeAlgo(context.self)
        idle(neighbours, receivedFromNeighbours, actorId)

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"Actor${context.self.path.name} received setActorID() from Actor${m5.sender.path.name}. Actor id set to: $actorId!")
        m5.sender ! WaveMain.setIdAckForTreeAlgo(context.self)
        idle(neighbours, receivedFromNeighbours, m5.actorId)

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
        
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled  
    }
  }

  private def active( parent: ActorRef[TreeAlgoMessage],
                      neighbours: ListBuffer[ActorRef[TreeAlgoMessage]],
                      actorId: Int
                    ): Behavior[TreeAlgoMessage] = {

    Behaviors.receiveMessage {

      case m2: SendTokenToNeighbour =>
        //in active state, token can be received only from parent
        assert(m2.sender == parent)
        context.log.info(s"ACTIVE: Actor${context.self.path.name} receives message from ${parent.path.name} and decides")
        spawnerActor ! WaveMain.shutSystemForTreeAlgo(context.self)
        passive()

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same

      case message =>
        context.log.warn(s"Warning! Active process received ${message.getClass()} message. This behavior is unhandled.")
        Behaviors.unhandled
    }
  }

  private def passive(): Behavior[TreeAlgoMessage] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }
}

