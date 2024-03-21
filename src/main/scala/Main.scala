package AkkaPac

import NetGraphAlgebraDefs.{Action, NetGraph, NetGraphComponent, NetModelAlgebra, NodeObject}

import java.io.{FileInputStream, ObjectInputStream}
import scala.jdk.CollectionConverters.*
import HelperFunctions.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.collection.mutable
import AkkaPac.TarryNode
//import NetGraphAlgebraDefs.NodeObject


//#greeter-main
object WaveMain{

  final case class bootSystem(nodeList: List[Int], edgeList: List[(Int, Int)])

  def apply(): Behavior[bootSystem] =
    Behaviors.setup { context =>
      val envTable = mutable.Map.empty[Int, ActorRef[TarryNode.TokenMessages]]

      Behaviors.receiveMessage { message =>
        //#create-actors
        for(nodeId <- message.nodeList){
          val newActor = context.spawn(TarryNode(), s"$nodeId")
          envTable += nodeId -> newActor
          newActor ! TarryNode.setActorID(nodeId)
        }

        for(edge <- message.edgeList) {
          val Actor1Id = edge._1
          val Actor2Id = edge._2

          val Actor1Ref = envTable(Actor1Id)
          val Actor2Ref = envTable(Actor2Id)

          Actor1Ref ! TarryNode.updateNeighbour(Actor1Id, Actor2Ref)
          Actor2Ref ! TarryNode.updateNeighbour(Actor2Id, Actor1Ref)
        }

        println(envTable)

        for(actorId <- envTable.keys){
          envTable(actorId) ! TarryNode.getNeighbours(actorId)
        }

        envTable(0) ! TarryNode.InitializeTraversal()

        Behaviors.same
      }
    }
}

object Main{
  def main(args: Array[String]): Unit = {

    val netGraph: Option[NetGraph] = load_graph(fileName = s"Graph6.1.ngs", dir = "./input/", FS = "local")

    val nodes :List[Int] = netGraph.get.sm.nodes().asScala.toList.map(n => n.id)
    val edges :List[(Int, Int)] = netGraph.get.sm.edges().asScala.toList.map(ep => (ep.nodeU.id, ep.nodeV.id))
    println(nodes)
    println(edges)

    println("Hello world!")

    //#actor-system
    val waveMain: ActorSystem[WaveMain.bootSystem] = ActorSystem(WaveMain(), "WaveStart")

    waveMain ! WaveMain.bootSystem(nodeList = nodes, edgeList = edges)
  }
}