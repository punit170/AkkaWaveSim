package AkkaPac

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.annotation.unused
import scala.collection.mutable.ListBuffer

trait commonWaveActor {
  trait TokenMessages
  final case class SendTokenToNeighbour(sender: ActorRef[WaveMessage]) extends TokenMessages
  final case class InitializeTraversal() extends TokenMessages

  trait Command
  final case class updateNeighbours(neighIds: List[ActorRef[WaveMessage]], sender: ActorRef[WaveMain.systemMessage]) extends Command
  final case class getNeighbours(actorId: Int) extends Command
  final case class setActorID(actorId: Int, sender: ActorRef[WaveMain.systemMessage]) extends Command
  final case class getActorID() extends Command

/*  @unused
  trait Ack
  @unused
  final case class setActorIdAck(sender: ActorRef[WaveMessage]) extends Ack
  @unused
  final case class setNeighbourAck(sender: ActorRef[WaveMessage]) extends Ack*/
  
  trait Information

  type WaveMessage = TokenMessages | Command | Information/* | Ack*/
}

/*
@unused
object accessCommonWaveActor extends commonWaveActor {}*/
