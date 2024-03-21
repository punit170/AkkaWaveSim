package AkkaPac

import AkkaPac.TarryNode.TokenMessages
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import org.jgrapht.util.DoublyLinkedList.ListNode

import scala.util.Random
import scala.collection.Set
import scala.collection.mutable.ListBuffer


object TarryNode{
  def apply(): Behavior[TokenMessages] =
    Behaviors.setup(context => new TarryNode(context))

  //message types
  sealed trait TokenMessages
  final private case class SendTokenToNeighbour(sender: ActorRef[TokenMessages]) extends TokenMessages
  final case class InitializeTraversal() extends TokenMessages
  final case class updateNeighbour(neighId: ActorRef[TokenMessages]) extends TokenMessages
  final case class getNeighbours(actorId: Int) extends TokenMessages
  final case class setActorID(actorId: Int) extends TokenMessages
  final case class getActorID() extends TokenMessages
}

class TarryNode(context: ActorContext[TarryNode.TokenMessages]) extends AbstractBehavior[TarryNode.TokenMessages](context){
  import TarryNode._
  //state- internal variables
  var seenToken: Boolean = false
  var parent: ActorRef[TokenMessages] = null
  val neighbours: ListBuffer[ActorRef[TokenMessages]] = ListBuffer.empty
  val neighboursVisited: ListBuffer[ActorRef[TokenMessages]] = ListBuffer.empty
  var actorId: Int = ???

  //Behaviour when receiving token messages
  override def onMessage(message: TokenMessages): Behavior[TokenMessages] =
    message match {
      case _: InitializeTraversal => {
        seenToken = true
        parent = null

        if (neighbours.isEmpty) {
          context.log.info("No neighbour for initial actor!")
          Behaviors.stopped
        }
        val sendToIndex = Random.nextInt(neighbours.length)
        val sendTo = neighbours(sendToIndex)

        neighboursVisited.addOne(sendTo)

        context.log.info(s"Initiator ${context.self} is sending token to $sendTo")
        sendTo ! SendTokenToNeighbour(context.self)
        Behaviors.same
      }

      case m2: SendTokenToNeighbour => {
        if (!seenToken) {
          context.log.info(s"${context.self} receives token for first time and makes ${m2.sender} its parent")
          seenToken = true
          parent = m2.sender
          assert(neighbours.contains(m2.sender))
          neighbours -= parent
        }

        if (!neighbours.isEmpty) {
          val sendToIndex = Random.nextInt(neighbours.length)
          val sendTo = neighbours(sendToIndex)
          neighbours -= sendTo
          neighboursVisited.addOne(sendTo)

          context.log.info(s"${context.self} is sending token to $sendTo")
          sendTo ! SendTokenToNeighbour(context.self)
          Behaviors.same
        }
        else if (parent != null) {
          context.log.info(s"${context.self} is sending token back to parent $parent")
          parent ! SendTokenToNeighbour(context.self)
          Behaviors.same
        }
        else {
          context.log.info("Traversal ends!!")
          Behaviors.stopped
        }
      }

      case m3: updateNeighbour => {
        neighbours += m3.neighId
        Behaviors.same
      }

      case m4: getNeighbours => {
        println(s"${m4.actorId} -> neighbours: ${neighbours}")
        Behaviors.same
      }

      case m5: setActorID => {
        actorId = m5.actorId
        Behaviors.same
      }

      case m6: getActorID => {
        println(s"My Actor Ref is: ${context.self} and my Actor ID is: $actorId")
        Behaviors.same
      }


    }
}

