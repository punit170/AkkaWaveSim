import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Counter {
  sealed trait Command
  case object Increment extends Command
  final case class GetValue(replyTo: ActorRef[Value]) extends Command
  final case class Value(n: Int)

  def apply(): Behavior[Command] =
    counter(0)

  private def counter(n: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increment =>
          val newValue = n + 1
          context.log.debug("Incremented counter to [{}]", newValue)
          counter(newValue)
        case GetValue(replyTo) =>
          replyTo ! Value(n)
          Behaviors.same
      }
    }
}