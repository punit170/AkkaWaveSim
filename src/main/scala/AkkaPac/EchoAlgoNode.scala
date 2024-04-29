package AkkaPac

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext

import scala.util.Random
import scala.collection.Set
import scala.collection.mutable.ListBuffer


object EchoAlgoNode extends commonWaveActor {

  //message for parent
//  final private case class SendTokenToParent(sender: ActorRef[EchoAlgoMessage]) extends TokenMessages

  //message types
  type EchoAlgoMessage = WaveMessage

  def apply(spawnerActor: ActorRef[WaveMain.systemMessage]): Behavior[EchoAlgoMessage] =
    Behaviors.setup { context =>
      new EchoAlgoNode(context, spawnerActor).idle(ListBuffer.empty[ActorRef[EchoAlgoMessage]] ,ListBuffer.empty[ActorRef[EchoAlgoMessage]], -1)
    }
}

class EchoAlgoNode(context: ActorContext[EchoAlgoNode.EchoAlgoMessage], spawnerActor: ActorRef[WaveMain.systemMessage]) {

  import EchoAlgoNode._

  //states: idle, active
  private def idle(
                    neighbours: ListBuffer[ActorRef[EchoAlgoMessage]],
                    receivedMessagesFrom: ListBuffer[ActorRef[EchoAlgoMessage]],
                    actorId: Int
                  ): Behavior[EchoAlgoMessage] = {

    Behaviors.receiveMessage {
      case _: InitializeTraversal =>
        if (neighbours.isEmpty) {
          context.log.info(s"No neighbour for initial actor ${context.self}!")
          passive()
        }

        context.log.info(s"IDLE: Initiator Actor${context.self.path.name} is sending token to all its neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")

        neighbours.foreach(n => n ! SendTokenToNeighbour(context.self))

        active(null, neighbours, ListBuffer.empty[ActorRef[EchoAlgoMessage]], actorId)

      case m2: SendTokenToNeighbour =>
        context.log.info(s"IDLE: Actor${context.self.path.name} received message from Actor${m2.sender.path.name} and making it as parent.")
        receivedMessagesFrom.addOne(m2.sender)

        val parent = m2.sender

        val otherNeighbours = neighbours.diff(List(parent))

        if (otherNeighbours.nonEmpty) {
          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token to all other neighbours: ${otherNeighbours.map(n => "Actor" + n.path.name)}")
          otherNeighbours.foreach(n => n ! SendTokenToNeighbour(context.self))

          active(parent, neighbours, receivedMessagesFrom, actorId)
        }
        else {
          context.log.info(s"IDLE: Actor${context.self.path.name} is sending token back to parent (only neighbour): Actor${parent.path.name}")
          parent ! SendTokenToNeighbour(context.self)
          passive()
        }

      case m3: updateNeighbours =>
        neighbours.addAll(m3.neighIds)
        context.log.info(s"Actor${context.self.path.name} received updateNeighbours() from Actor${m3.sender.path.name}. Neighbours updated: ${neighbours.map(n => "Actor" + n.path.name)}!")
        m3.sender ! WaveMain.setNeighbourAckForEchoAlgo(context.self)
        idle(neighbours, receivedMessagesFrom, actorId)

      case m4: getNeighbours =>
        context.log.info(s"Actor${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case m5: setActorID =>
        context.log.info(s"Actor${context.self.path.name} received setActorID() from Actor${m5.sender.path.name}. Actor id set to: $actorId!")
        m5.sender ! WaveMain.setIdAckForEchoAlgo(context.self)
        idle(neighbours, receivedMessagesFrom, m5.actorId)

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
        
      case _ =>
        context.log.warn("Unknown Message Type!")
        Behaviors.unhandled  
    }
    
  }

  private def active( parent: ActorRef[EchoAlgoMessage],
                      neighbours: ListBuffer[ActorRef[EchoAlgoMessage]],
                      receivedMessagesFrom: ListBuffer[ActorRef[EchoAlgoMessage]],
                      actorId: Int
                    ): Behavior[EchoAlgoMessage] = {

    Behaviors.receiveMessage {
          
      case m2: SendTokenToNeighbour =>
        receivedMessagesFrom.addOne(m2.sender)
        context.log.info(s"ACTIVE: Actor${context.self.path.name} receives message from Actor${m2.sender.path.name}, neighbours: ${neighbours.map(n => "Actor" + n.path.name)}, receivedMessagesFrom: ${receivedMessagesFrom.map(n => "Actor" + n.path.name)}")

        if (neighbours.diff(receivedMessagesFrom).isEmpty) {
          if (parent != null) {
            context.log.info(s"ACTIVE: Actor${context.self.path.name} received back messages from all its neighbours. It is sending back message to its parent Actor${parent.path.name}")
            parent ! SendTokenToNeighbour(context.self)
            Behaviors.same
          }
          else {
            context.log.info(s"ACTIVE: Initiator Actor${context.self.path.name} received back messages from all its neighbours. Echo Traversal ends!")
            spawnerActor ! WaveMain.shutSystemForEchoAlgo(context.self)
            passive()
          }
        }
        else {
          Behaviors.same
        }

      case m4: getNeighbours =>
        context.log.info(s"${m4.actorId} -> neighbours: ${neighbours.map(n => "Actor" + n.path.name)}")
        Behaviors.same

      case _: getActorID =>
        context.log.info(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same

      case message =>
        context.log.warn(s"Warning! Active process received ${message.getClass()} message. This behavior is unhandled.")
        Behaviors.unhandled

    }
  }

  private def passive(): Behavior[EchoAlgoMessage] = {
    Behaviors.receiveMessage { _ =>
      context.log.warn("Warning! No message should be received in the passive state.")
      Behaviors.stopped
    }
  }
}

