import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestDuration, LoggingTestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import AkkaPac.*
import AkkaPac.HelperFunctions.load_graph
import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import scala.jdk.CollectionConverters.*

class AsyncTestingSpec
  extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers {
  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  override def afterAll(): Unit = testKit.shutdownTestKit()
}

class WaveAlgoTests extends AsyncTestingSpec {

  "log info" should {
    "DFSAlgo will receive setActorId send from MainActor!" in {
      val nodes: List[Int] = List(0)
      val neighbourMap: Map[Int, List[Int]] = Map((0, List()))
      val mainActor: ActorRef[WaveMain.systemMessage] = testKit.spawn(WaveMain(nodes, neighbourMap))

      val dfsNodeActor = testKit.spawn(DFSNode(mainActor))
//      val dfsActorProbe = testKit.createTestProbe[DFSNode.DFSMessage]()

      dfsNodeActor ! DFSNode.setActorID(1, mainActor)

      LoggingTestKit.info("received setActorID()!")
    }
  }

  "load_graph" should {
    "load a graph from a file" in {
      import java.io.File

      // Provide a test graph file
      val fileName = "Graph7.ngs"
      val dir = "./input/"

      val file = new File(s"$dir/$fileName")
      val fileExists = file.exists()

      if (fileExists) {
        // Load the graph using load_graph function
        val netGraph = load_graph(fileName, dir)
        netGraph shouldBe defined
        println(s"Graph Nodes: ${netGraph.get.sm.nodes.asScala.toList.map(n => n.id)}")
        println(s"Graph Edges: ${netGraph.get.sm.edges.asScala.toList.map(ep => (ep.nodeU.id, ep.nodeV.id))}")
      }
    }
  }

  "log warning" should {
    "send TarryAlgo InitializeTraversal message to an actor in passive state" in {
      val nodes: List[Int] = List(0)
      val neighbourMap: Map[Int, List[Int]] = Map((0, List()))
      val mainActor: ActorRef[WaveMain.systemMessage] = testKit.spawn(WaveMain(nodes, neighbourMap))

      val tarryNodeActor = testKit.spawn(TarryNode(mainActor))
//      val TarryActorProbe = testKit.createTestProbe[TarryNode.Message]()

      tarryNodeActor ! TarryNode.setActorID(0, mainActor)
      tarryNodeActor ! TarryNode.updateNeighbours(List.empty, mainActor)
      tarryNodeActor ! TarryNode.InitializeTraversal()

      LoggingTestKit.warn("Warning!")
    }
  }

}

