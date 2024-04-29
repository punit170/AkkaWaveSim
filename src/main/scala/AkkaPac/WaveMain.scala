package AkkaPac

import AkkaPac.*
import AkkaPac.HelperFunctions.*
import NetGraphAlgebraDefs.*
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.StdIn.readInt
import scala.jdk.CollectionConverters.*
import scala.util.Random

//Main actor- responsible for dynamically setting up and deleting distributed network environments for simulations
object WaveMain {

  sealed trait bootSystemMessage
  final case class bootSystemForTarryAlgo() extends bootSystemMessage
  final case class bootSystemForDFSAlgo() extends bootSystemMessage
  final case class bootSystemForOptimizedDFSAlgo() extends bootSystemMessage
  final case class bootSystemForAwerbuchDFSAlgo() extends bootSystemMessage
  final case class bootSystemForCidonDFSAlgo() extends bootSystemMessage
  final case class bootSystemForTreeAlgo() extends bootSystemMessage
  final case class bootSystemForEchoAlgo() extends bootSystemMessage


  sealed trait shutSystemMessage
  final case class shutSystemForTarryAlgo(sender: ActorRef[TarryNode.Message]) extends shutSystemMessage
  final case class shutSystemForDFSAlgo(sender: ActorRef[DFSNode.DFSMessage]) extends shutSystemMessage
  final case class shutSystemForOptimizedDFSAlgo(sender: ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]) extends shutSystemMessage
  final case class shutSystemForAwerbuchDFSAlgo(sender: ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]) extends shutSystemMessage
  final case class shutSystemForCidonDFSAlgo(sender: ActorRef[CidonDFSNode.CidonDFSNodeMessage]) extends shutSystemMessage
  final case class shutSystemForTreeAlgo(sender: ActorRef[TreeAlgoNode.TreeAlgoMessage]) extends shutSystemMessage
  final case class shutSystemForEchoAlgo(sender: ActorRef[EchoAlgoNode.EchoAlgoMessage]) extends shutSystemMessage

  sealed trait actorTerminatedAck
  final private case class terminateAckForTarryAlgo(stoppedActRef: ActorRef[TarryNode.Message]) extends actorTerminatedAck
  final private case class terminateAckForDFSAlgo(stoppedActRef: ActorRef[DFSNode.DFSMessage]) extends actorTerminatedAck
  final private case class terminateAckForOptimizedDFSAlgo(stoppedActRef: ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]) extends actorTerminatedAck
  final private case class terminateAckForAwerbuchDFSAlgo(stoppedActRef: ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]) extends actorTerminatedAck
  final private case class terminateAckForCidonDFSAlgo(stoppedActRef: ActorRef[CidonDFSNode.CidonDFSNodeMessage]) extends actorTerminatedAck
  final private case class terminateAckForTreeAlgo(stoppedActRef: ActorRef[TreeAlgoNode.TreeAlgoMessage]) extends actorTerminatedAck
  final private case class terminateAckForEchoAlgo(stoppedActRef: ActorRef[EchoAlgoNode.EchoAlgoMessage]) extends actorTerminatedAck

  sealed trait setIdAck
  final case class setIdAckForTarryAlgo(sender: ActorRef[TarryNode.Message]) extends setIdAck
  final case class setIdAckForDFSAlgo(sender: ActorRef[DFSNode.DFSMessage]) extends setIdAck
  final case class setIdAckForOptimizedDFSAlgo(sender: ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]) extends setIdAck
  final case class setIdAckForAwerbuchDFSAlgo(sender: ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]) extends setIdAck
  final case class setIdAckForCidonDFSAlgo(sender: ActorRef[CidonDFSNode.CidonDFSNodeMessage]) extends setIdAck
  final case class setIdAckForTreeAlgo(sender: ActorRef[TreeAlgoNode.TreeAlgoMessage]) extends setIdAck
  final case class setIdAckForEchoAlgo(sender: ActorRef[EchoAlgoNode.EchoAlgoMessage]) extends setIdAck

  sealed trait setNeighbourAck
  final case class setNeighbourAckForTarryAlgo(sender: ActorRef[TarryNode.Message]) extends setNeighbourAck
  final case class setNeighbourAckForDFSAlgo(sender: ActorRef[DFSNode.DFSMessage]) extends setNeighbourAck
  final case class setNeighbourAckForOptimizedDFSAlgo(sender: ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]) extends setNeighbourAck
  final case class setNeighbourAckForAwerbuchDFSAlgo(sender: ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]) extends setNeighbourAck
  final case class setNeighbourAckForCidonDFSAlgo(sender: ActorRef[CidonDFSNode.CidonDFSNodeMessage]) extends setNeighbourAck
  final case class setNeighbourAckForTreeAlgo(sender: ActorRef[TreeAlgoNode.TreeAlgoMessage]) extends setNeighbourAck
  final case class setNeighbourAckForEchoAlgo(sender: ActorRef[EchoAlgoNode.EchoAlgoMessage]) extends setNeighbourAck

  sealed trait Command
  final case class nextSimulation() extends Command


  type systemMessage = bootSystemMessage | shutSystemMessage | setIdAck | setNeighbourAck | actorTerminatedAck | Command

  def apply(nodes: List[Int], neighbourMap: Map[Int, List[Int]]): Behavior[WaveMain.systemMessage] = {
    Behaviors.setup { context =>
      case class actorEnvState[T](envTable: mutable.Map[Int, ActorRef[T]], spawnedActors: ListBuffer[ActorRef[T]], actorTerminateAckFrom:ListBuffer[ActorRef[T]], setActorIdAckFrom: ListBuffer[ActorRef[T]], setNeighboursAckFrom: ListBuffer[ActorRef[T]], leafNodes: ListBuffer[Int], neighbourMap: mutable.Map[Int, List[Int]])

      lazy val actorEnvStateForTarryAlgo = actorEnvState[TarryNode.Message](mutable.Map.empty[Int, ActorRef[TarryNode.Message]], ListBuffer.empty[ActorRef[TarryNode.Message]], ListBuffer.empty[ActorRef[TarryNode.Message]], ListBuffer.empty[ActorRef[TarryNode.Message]], ListBuffer.empty[ActorRef[TarryNode.Message]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])
      lazy val actorEnvStateForDFSAlgo = actorEnvState[DFSNode.DFSMessage](mutable.Map.empty[Int, ActorRef[DFSNode.DFSMessage]], ListBuffer.empty[ActorRef[DFSNode.DFSMessage]], ListBuffer.empty[ActorRef[DFSNode.DFSMessage]], ListBuffer.empty[ActorRef[DFSNode.DFSMessage]], ListBuffer.empty[ActorRef[DFSNode.DFSMessage]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])
      lazy val actorEnvStateForOptimizedDFSAlgo = actorEnvState[OptimizedDFSNode.OptimizedDFSNodeMessage](mutable.Map.empty[Int, ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]], ListBuffer.empty[ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]], ListBuffer.empty[ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]], ListBuffer.empty[ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]], ListBuffer.empty[ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])
      lazy val actorEnvStateForAwerbuchDFSAlgo = actorEnvState[AwerbuchDFSNode.AwerbuchDFSNodeMessage](mutable.Map.empty[Int, ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]], ListBuffer.empty[ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]], ListBuffer.empty[ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]], ListBuffer.empty[ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]], ListBuffer.empty[ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])
      lazy val actorEnvStateForCidonDFSAlgo = actorEnvState[CidonDFSNode.CidonDFSNodeMessage](mutable.Map.empty[Int, ActorRef[CidonDFSNode.CidonDFSNodeMessage]], ListBuffer.empty[ActorRef[CidonDFSNode.CidonDFSNodeMessage]], ListBuffer.empty[ActorRef[CidonDFSNode.CidonDFSNodeMessage]], ListBuffer.empty[ActorRef[CidonDFSNode.CidonDFSNodeMessage]], ListBuffer.empty[ActorRef[CidonDFSNode.CidonDFSNodeMessage]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])
      lazy val actorEnvStateForTreeAlgo = actorEnvState[TreeAlgoNode.TreeAlgoMessage](mutable.Map.empty[Int, ActorRef[TreeAlgoNode.TreeAlgoMessage]], ListBuffer.empty[ActorRef[TreeAlgoNode.TreeAlgoMessage]], ListBuffer.empty[ActorRef[TreeAlgoNode.TreeAlgoMessage]], ListBuffer.empty[ActorRef[TreeAlgoNode.TreeAlgoMessage]], ListBuffer.empty[ActorRef[TreeAlgoNode.TreeAlgoMessage]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])
      lazy val actorEnvStateForEchoAlgo = actorEnvState[EchoAlgoNode.EchoAlgoMessage](mutable.Map.empty[Int, ActorRef[EchoAlgoNode.EchoAlgoMessage]], ListBuffer.empty[ActorRef[EchoAlgoNode.EchoAlgoMessage]], ListBuffer.empty[ActorRef[EchoAlgoNode.EchoAlgoMessage]], ListBuffer.empty[ActorRef[EchoAlgoNode.EchoAlgoMessage]], ListBuffer.empty[ActorRef[EchoAlgoNode.EchoAlgoMessage]], ListBuffer.empty[Int], mutable.Map.empty[Int, List[Int]])

      Behaviors.receiveMessage { message =>
        /* T- actorMessageType
         * U- setActorMessageType
         * V- updateNeighbourMessageType
         * W- getNeighboursMessageType
         * X- initializeTraversalMessageType
        */
        def runWaveAlgo[T, U <: T, V <: T, W <: T](nodes: List[Int],
                                                   actorNodeFunc: () => Behavior[T],
                                                   actorEnv: actorEnvState[T],
                                                   setActorMessageFunc: (Int, ActorRef[systemMessage]) => U,
                                                   updateNeighbourMessageFunc: (List[ActorRef[T]], ActorRef[systemMessage]) => V,
                                                   getNeighboursMessageFunc: Int => W
                                                   ): Unit = {
          context.log.info("Spawing New Actor, sending messages to set their ids & neighbours...")
          for (nodeId <- nodes) {
            val newActor = context.spawn(actorNodeFunc(), s"$nodeId")
            actorEnv.envTable += nodeId -> newActor
            actorEnv.spawnedActors += newActor
            newActor ! setActorMessageFunc(nodeId, context.self)
          }

          val neighbourMapForActors = actorEnv.neighbourMap.map(ele => actorEnv.envTable(ele._1) -> ele._2.map(nId => actorEnv.envTable(nId)))

          neighbourMapForActors.keys.foreach(actor => {
            actor ! updateNeighbourMessageFunc(neighbourMapForActors(actor), context.self)
          })

          actorEnv.envTable.keys.foreach(actorId => actorEnv.envTable(actorId) ! getNeighboursMessageFunc(actorId))

        }

        def runPostOnlineAck[T, X<:T](isSetActorAck: Boolean, sender: ActorRef[T], isTreeAlgo: Boolean, actorEnv :actorEnvState[T], initTravMessageFunc:  () => X): Unit = {
          if (isSetActorAck) {
            context.log.info("")
            context.log.info(s"idAckMessage received from Actor${sender.path.name}")
            actorEnv.setActorIdAckFrom += sender
          } else {
            context.log.info("")
            context.log.info(s"neighbourAckMessage received from Actor${sender.path.name}")
            actorEnv.setNeighboursAckFrom += sender
          }

          context.log.info(s"idAckFrom: ${actorEnv.setActorIdAckFrom.map(actRef => actRef.path.name)}")
          context.log.info(s"neighboursAckFrom: ${actorEnv.setNeighboursAckFrom.map(actRef => actRef.path.name)}")

          if (actorEnv.spawnedActors.diff(actorEnv.setActorIdAckFrom).isEmpty && actorEnv.spawnedActors.diff(actorEnv.setNeighboursAckFrom).isEmpty) {
            context.log.info(s"spawnedActors: ${actorEnv.spawnedActors.map(n => "Actor"+ n.path.name)}")
            context.log.info(s"setActorIdAckFrom: ${actorEnv.setActorIdAckFrom.map(n => "Actor"+ n.path.name)}")
            context.log.info(s"setNeighboursAckFrom: ${actorEnv.setNeighboursAckFrom.map(n => "Actor"+ n.path.name)}")

            context.log.info("")
            context.log.info("All setActorIdAck and setNeighbourAck received! Initiating algorithm...")
            //initialize traversal

            if(isTreeAlgo){
              for (leafNodeId <- actorEnv.leafNodes)
                actorEnv.envTable(leafNodeId) ! initTravMessageFunc()
            }
            else {
              if (actorEnv.envTable.isEmpty)
                throw new Exception("Neighbour Map is empty! Probable reason: Improper Usage!")

              val randNodeKey = Random.nextInt(actorEnv.envTable.size)
              actorEnv.envTable(randNodeKey) ! initTravMessageFunc()
            }
          }
        }

        def runPostStopAck[T](stoppedActRef: ActorRef[T], actorEnv: actorEnvState[T]): Unit = {
          context.log.info(s"Actor${stoppedActRef.path.name} stopped!")
          actorEnv.actorTerminateAckFrom += stoppedActRef
          if (actorEnv.spawnedActors.diff(actorEnv.actorTerminateAckFrom).isEmpty) {
            actorEnv.productIterator.foreach {
              case lst: ListBuffer[_] => lst.clear()
              case map: mutable.Map[_, _] => map.clear()
              case _ =>
            }
            context.self ! nextSimulation()
          }
        }

        def createTree(graphNeighbourMap: Map[Int, List[Int]]) = {
          val discovered = ListBuffer.empty[Int]
          val fullyExplored = ListBuffer.empty[Int]
          val treeEdges = ListBuffer.empty[(Int, Int)]

          def DFS(root: Int): Unit = {
            discovered += root
            val neighbours = graphNeighbourMap(root)
            neighbours.foreach(n => {
              if(!discovered.contains(n)) {
                treeEdges += ((root, n))
                DFS(n)
              }}
            )
            fullyExplored += root
          }

          DFS(0)

          val undirectedTreeEdges = treeEdges.foldLeft(Set.empty[(Int, Int)])((acc, ep) => {
            acc + ((ep._1, ep._2)) + ((ep._2, ep._1))
          })

          val treeEdgesMap = undirectedTreeEdges.foldLeft(Map.empty[Int, ListBuffer[Int]])((acc, ep) => {
            if (acc.contains(ep._1)) {
              acc(ep._1) += ep._2
              acc
            }
            else {
              acc + (ep._1 -> ListBuffer(ep._2))
            }
          }).map(ele => ele._1 -> ele._2.toList)

          (discovered.toList, treeEdgesMap, fullyExplored.toList)
        }

        message match {
          case _: bootSystemForTarryAlgo =>
            context.log.info(s"##########################################")
            context.log.info("Boot system for Tarry Algo message received!\n")

            runWaveAlgo[TarryNode.Message,
                        TarryNode.setActorID,
                        TarryNode.updateNeighbours,
                        TarryNode.getNeighbours
                      ](nodes, () => TarryNode(context.self), actorEnvStateForTarryAlgo, (id: Int, sender: ActorRef[WaveMain.systemMessage]) => TarryNode.setActorID(id, sender), (LsActRef: List[ActorRef[TarryNode.Message]], sender: ActorRef[systemMessage]) => TarryNode.updateNeighbours(LsActRef, sender), (id: Int) => TarryNode.getNeighbours(id))
            Behaviors.same
          case _: bootSystemForDFSAlgo =>
            context.log.info(s"##########################################")
            context.log.info("Boot system for DFS Algo message received!\n")

            runWaveAlgo[DFSNode.DFSMessage,
                        DFSNode.setActorID,
                        DFSNode.updateNeighbours,
                        DFSNode.getNeighbours
                      ](nodes, () => DFSNode(context.self), actorEnvStateForDFSAlgo, (id: Int, sender: ActorRef[systemMessage]) => DFSNode.setActorID(id, sender), (LsActRef: List[ActorRef[DFSNode.DFSMessage]], sender: ActorRef[systemMessage]) => DFSNode.updateNeighbours(LsActRef, sender), (id: Int) => DFSNode.getNeighbours(id))
            Behaviors.same
          case _: bootSystemForOptimizedDFSAlgo =>
            runWaveAlgo[OptimizedDFSNode.OptimizedDFSNodeMessage,
                        OptimizedDFSNode.setActorID,
                        OptimizedDFSNode.updateNeighbours,
                        OptimizedDFSNode.getNeighbours
                      ](nodes, () => OptimizedDFSNode(context.self), actorEnvStateForOptimizedDFSAlgo, (id: Int, sender: ActorRef[systemMessage]) => OptimizedDFSNode.setActorID(id, sender), (LsActRef: List[ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage]], sender: ActorRef[systemMessage]) => OptimizedDFSNode.updateNeighbours(LsActRef, sender), (id: Int) => OptimizedDFSNode.getNeighbours(id))
            Behaviors.same
          case _: bootSystemForAwerbuchDFSAlgo =>
            runWaveAlgo[AwerbuchDFSNode.AwerbuchDFSNodeMessage,
                        AwerbuchDFSNode.setActorID,
                        AwerbuchDFSNode.updateNeighbours,
                        AwerbuchDFSNode.getNeighbours
                      ](nodes, () => AwerbuchDFSNode(context.self), actorEnvStateForAwerbuchDFSAlgo, (id: Int, sender: ActorRef[systemMessage]) => AwerbuchDFSNode.setActorID(id, sender), (LsActRef: List[ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage]], sender: ActorRef[systemMessage]) => AwerbuchDFSNode.updateNeighbours(LsActRef, sender), (id: Int) => AwerbuchDFSNode.getNeighbours(id))
            Behaviors.same
          case _: bootSystemForCidonDFSAlgo =>
            runWaveAlgo[CidonDFSNode.CidonDFSNodeMessage,
                        CidonDFSNode.setActorID,
                        CidonDFSNode.updateNeighbours,
                        CidonDFSNode.getNeighbours
                      ](nodes, () => CidonDFSNode(context.self), actorEnvStateForCidonDFSAlgo, (id: Int, sender: ActorRef[systemMessage]) => CidonDFSNode.setActorID(id, sender), (LsActRef: List[ActorRef[CidonDFSNode.CidonDFSNodeMessage]], sender: ActorRef[systemMessage]) => CidonDFSNode.updateNeighbours(LsActRef, sender), (id: Int) => CidonDFSNode.getNeighbours(id))
            Behaviors.same
          case _: bootSystemForTreeAlgo =>
            runWaveAlgo[TreeAlgoNode.TreeAlgoMessage,
                        TreeAlgoNode.setActorID,
                        TreeAlgoNode.updateNeighbours,
                        TreeAlgoNode.getNeighbours
                      ](nodes, () => TreeAlgoNode(context.self), actorEnvStateForTreeAlgo, (id: Int, sender: ActorRef[systemMessage]) => TreeAlgoNode.setActorID(id, sender), (LsActRef: List[ActorRef[TreeAlgoNode.TreeAlgoMessage]], sender: ActorRef[systemMessage]) => TreeAlgoNode.updateNeighbours(LsActRef, sender), (id: Int) => TreeAlgoNode.getNeighbours(id))
            Behaviors.same
          case _: bootSystemForEchoAlgo =>
            runWaveAlgo[EchoAlgoNode.EchoAlgoMessage,
                        EchoAlgoNode.setActorID,
                        EchoAlgoNode.updateNeighbours,
                        EchoAlgoNode.getNeighbours
                      ](nodes, () => EchoAlgoNode(context.self), actorEnvStateForEchoAlgo, (id: Int, sender: ActorRef[systemMessage]) => EchoAlgoNode.setActorID(id, sender), (LsActRef: List[ActorRef[EchoAlgoNode.EchoAlgoMessage]], sender: ActorRef[systemMessage]) => EchoAlgoNode.updateNeighbours(LsActRef, sender), (id: Int) => EchoAlgoNode.getNeighbours(id))
            Behaviors.same

          case setActorAckMessage: setIdAck =>
            setActorAckMessage match {
              case c1: setIdAckForTarryAlgo =>
                context.log.info(s"received setIdAckMessage from ${c1.sender.path.name}")
                runPostOnlineAck(true, c1.sender, false, actorEnvStateForTarryAlgo, () => TarryNode.InitializeTraversal())
              case c2: setIdAckForDFSAlgo =>
                context.log.info(s"received setIdAckMessage from ${c2.sender.path.name}")
                runPostOnlineAck(true, c2.sender, false, actorEnvStateForDFSAlgo, () => DFSNode.InitializeTraversal())
              case c3: setIdAckForOptimizedDFSAlgo =>
                context.log.info(s"received setIdAckMessage from ${c3.sender.path.name}")
                runPostOnlineAck(true, c3.sender, false, actorEnvStateForOptimizedDFSAlgo, () => OptimizedDFSNode.InitializeTraversal())
              case c4: setIdAckForAwerbuchDFSAlgo =>
                context.log.info(s"received setIdAckMessage from ${c4.sender.path.name}")
                runPostOnlineAck(true, c4.sender, false, actorEnvStateForAwerbuchDFSAlgo, () => AwerbuchDFSNode.InitializeTraversal())
              case c5: setIdAckForCidonDFSAlgo =>
                context.log.info(s"received setIdAckMessage from ${c5.sender.path.name}")
                runPostOnlineAck(true, c5.sender, false, actorEnvStateForCidonDFSAlgo, () => CidonDFSNode.InitializeTraversal())
              case c6: setIdAckForTreeAlgo =>
                context.log.info(s"received setIdAckMessage from ${c6.sender.path.name}")
                runPostOnlineAck(true, c6.sender,true, actorEnvStateForTreeAlgo, () => TreeAlgoNode.InitializeTraversal())
              case c7: setIdAckForEchoAlgo =>
                context.log.info(s"received setIdAckMessage from ${c7.sender.path.name}")
                runPostOnlineAck(true, c7.sender, false, actorEnvStateForEchoAlgo, () => EchoAlgoNode.InitializeTraversal())
            }
            Behaviors.same

          case setNeighbourAckMessage: setNeighbourAck =>
            setNeighbourAckMessage match {
              case c1: setNeighbourAckForTarryAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c1.sender.path.name}")
                runPostOnlineAck(false, c1.sender, false, actorEnvStateForTarryAlgo, () => TarryNode.InitializeTraversal())
              case c2: setNeighbourAckForDFSAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c2.sender.path.name}")
                runPostOnlineAck(false, c2.sender, false, actorEnvStateForDFSAlgo, () => DFSNode.InitializeTraversal())
              case c3: setNeighbourAckForOptimizedDFSAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c3.sender.path.name}")
                runPostOnlineAck(false, c3.sender, false, actorEnvStateForOptimizedDFSAlgo, () => OptimizedDFSNode.InitializeTraversal())
              case c4: setNeighbourAckForAwerbuchDFSAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c4.sender.path.name}")
                runPostOnlineAck(false, c4.sender, false, actorEnvStateForAwerbuchDFSAlgo, () => AwerbuchDFSNode.InitializeTraversal())
              case c5: setNeighbourAckForCidonDFSAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c5.sender.path.name}")
                runPostOnlineAck(false, c5.sender, false, actorEnvStateForCidonDFSAlgo, () => CidonDFSNode.InitializeTraversal())
              case c6: setNeighbourAckForTreeAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c6.sender.path.name}")
                runPostOnlineAck(false, c6.sender,true, actorEnvStateForTreeAlgo, () => TreeAlgoNode.InitializeTraversal())
              case c7: setNeighbourAckForEchoAlgo =>
                context.log.info(s"received setNeighbourAckMessage from ${c7.sender.path.name}")
                runPostOnlineAck(false, c7.sender, false, actorEnvStateForEchoAlgo, () => EchoAlgoNode.InitializeTraversal())
            }
            Behaviors.same

          case shutMessage: shutSystemMessage =>

            shutMessage match {
              case _: shutSystemForTarryAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all TarryAlgo actors: ${context.children.map(n => "Actor" + n.path.name)}...!")
                val childrenToTerminate = context.children.collect{
                  case c: ActorRef[TarryNode.Message] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForTarryAlgo(actRef))
                  context.stop(actRef)
                })

              case _: shutSystemForDFSAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all DFSAlgo actors: ${context.children}...!")
                val childrenToTerminate = context.children.collect {
                  case c: ActorRef[DFSNode.DFSMessage] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForDFSAlgo(actRef))
                  context.stop(actRef)
                })
              case _: shutSystemForOptimizedDFSAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all OptimizedDFSAlgo actors: ${context.children}...!")
                val childrenToTerminate = context.children.collect {
                  case c: ActorRef[OptimizedDFSNode.OptimizedDFSNodeMessage] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForOptimizedDFSAlgo(actRef))
                  context.stop(actRef)
                })
              case _: shutSystemForAwerbuchDFSAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all AwerbuchDFSAlgo actors: ${context.children}...!")
                val childrenToTerminate = context.children.collect {
                  case c: ActorRef[AwerbuchDFSNode.AwerbuchDFSNodeMessage] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForAwerbuchDFSAlgo(actRef))
                  context.stop(actRef)
                })
              case _: shutSystemForCidonDFSAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all CidonDFSAlgo actors: ${context.children}...!")
                val childrenToTerminate = context.children.collect {
                  case c: ActorRef[CidonDFSNode.CidonDFSNodeMessage] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForCidonDFSAlgo(actRef))
                  context.stop(actRef)
                })
              case _: shutSystemForTreeAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all TreeAlgo actors: ${context.children}...!")
                val childrenToTerminate = context.children.collect {
                  case c: ActorRef[TreeAlgoNode.TreeAlgoMessage] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForTreeAlgo(actRef))
                  context.stop(actRef)
                })
              case _: shutSystemForEchoAlgo =>
                context.log.info("")
                context.log.info(s"Shutting down all EchoAlgo actors: ${context.children}...!")
                val childrenToTerminate = context.children.collect {
                  case c: ActorRef[EchoAlgoNode.EchoAlgoMessage] => c
                }
                childrenToTerminate.foreach(actRef => {
                  context.watchWith(actRef, terminateAckForEchoAlgo(actRef))
                  context.stop(actRef)
                })
            }
            Behaviors.same
          case termMsgForTarry: terminateAckForTarryAlgo =>
            runPostStopAck(termMsgForTarry.stoppedActRef, actorEnvStateForTarryAlgo)
            Behaviors.same
            
          case termMsgForDFS: terminateAckForDFSAlgo =>
            runPostStopAck(termMsgForDFS.stoppedActRef, actorEnvStateForDFSAlgo)
            Behaviors.same
            
          case termMsgForOptimizedDFS: terminateAckForOptimizedDFSAlgo =>
            runPostStopAck(termMsgForOptimizedDFS.stoppedActRef, actorEnvStateForOptimizedDFSAlgo)
            Behaviors.same
            
          case termMsgForAwerbuchDFS: terminateAckForAwerbuchDFSAlgo =>
            runPostStopAck(termMsgForAwerbuchDFS.stoppedActRef, actorEnvStateForAwerbuchDFSAlgo)
            Behaviors.same
            
          case termMsgForCidonDFS: terminateAckForCidonDFSAlgo =>
            runPostStopAck(termMsgForCidonDFS.stoppedActRef, actorEnvStateForCidonDFSAlgo)
            Behaviors.same
            
          case termMsgForTreeAlgo: terminateAckForTreeAlgo =>
            runPostStopAck(termMsgForTreeAlgo.stoppedActRef, actorEnvStateForTreeAlgo)
            Behaviors.same
            
          case termMsgForEchoAlgo: terminateAckForEchoAlgo =>
            runPostStopAck(termMsgForEchoAlgo.stoppedActRef, actorEnvStateForEchoAlgo)
            Behaviors.same
            
          case _: nextSimulation =>
            context.log.info("\n\n")
            context.log.info("Welcome to AkkaWaveSim")
            context.log.info("Select one from the following to initiate the corresponding wave algorithm simulation: ")
            context.log.info("1. Tarry's Algo [1]\t2. DFS Algo [2]\t3. Optimized DFS Algo [3]")
            context.log.info("4. Awerbuch's Algo [4]\t5. Cidon's Algo [5]")
            context.log.info("6. Tree Algo [6]\t7. Echo Algo [7]")
            context.log.info("Enter choice[1-7]: ")

            val algoChoice: Int = readInt()
            assert(algoChoice >= 1 && algoChoice <= 7)
            println()

            val algoStartMessage: WaveMain.bootSystemMessage = algoChoice match {
              case 1 =>
                actorEnvStateForTarryAlgo.neighbourMap.addAll(neighbourMap)
                WaveMain.bootSystemForTarryAlgo()
              case 2 =>
                actorEnvStateForDFSAlgo.neighbourMap.addAll(neighbourMap)
                WaveMain.bootSystemForDFSAlgo()
              case 3 =>
                actorEnvStateForOptimizedDFSAlgo.neighbourMap.addAll(neighbourMap)
                WaveMain.bootSystemForOptimizedDFSAlgo()
              case 4 =>
                actorEnvStateForAwerbuchDFSAlgo.neighbourMap.addAll(neighbourMap)
                WaveMain.bootSystemForAwerbuchDFSAlgo()
              case 5 =>
                actorEnvStateForCidonDFSAlgo.neighbourMap.addAll(neighbourMap)
                WaveMain.bootSystemForCidonDFSAlgo()
              case 6 =>
                val treeAttributes = createTree(neighbourMap)

                println(s"treeAttributes: $treeAttributes")
                println(s"leafNodes: ${actorEnvStateForTreeAlgo.leafNodes}")

                actorEnvStateForTreeAlgo.neighbourMap.addAll(treeAttributes._2)
                treeAttributes._2.foreach(ele =>
                  if (ele._2.length == 1) {
                    actorEnvStateForTreeAlgo.leafNodes += ele._1
                  }
                )
                WaveMain.bootSystemForTreeAlgo()
              case 7 =>
                actorEnvStateForEchoAlgo.neighbourMap.addAll(neighbourMap)
                WaveMain.bootSystemForEchoAlgo()
            }

            context.self ! algoStartMessage

            Behaviors.same
        }
      }
    }
  }
}