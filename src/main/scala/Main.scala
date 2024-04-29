import AkkaPac._
import HelperFunctions._
import NetGraphAlgebraDefs.NetGraph
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.jdk.CollectionConverters.*
import scala.collection.mutable.ListBuffer

object Main{
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val graphDir = config.getString("graphConfig.ngsGraphDir")
    val graphFileName = config.getString("graphConfig.ngsGraphFileName")

    val netGraph: Option[NetGraph] = load_graph(fileName = graphFileName, dir = graphDir)

    val nodes :List[Int] = netGraph.get.sm.nodes().asScala.toList.map(n => n.id)
    val edges :List[(Int, Int)] = netGraph.get.sm.edges().asScala.toList.map(ep => (ep.nodeU.id, ep.nodeV.id))
    val undirectedEdges = edges.foldLeft(Set.empty[(Int, Int)])((acc, ep) => {
      acc + ((ep._1, ep._2)) + ((ep._2, ep._1))
    })

    //creating neighbour map with an undirected network
    val neighbourMap: Map[Int, List[Int]] = undirectedEdges.foldLeft(Map.empty[Int, ListBuffer[Int]])((acc, ep) => {
      if(acc.contains(ep._1)){
        acc(ep._1) += ep._2
        acc
      }
      else{
        acc + (ep._1 -> ListBuffer(ep._2))
      }
    }).map(ele => ele._1 -> ele._2.toList)

    //#actor-system
    val waveMain: ActorSystem[WaveMain.systemMessage] = ActorSystem(WaveMain(nodes, neighbourMap), "WaveStart")

    waveMain.log.info("")
    waveMain.log.info(s"nodes: $nodes")
    waveMain.log.info(s"edges: $edges")
    waveMain.log.info(s"neighbourMap: $neighbourMap")

    waveMain ! WaveMain.nextSimulation()
  }
}
