package AkkaPac

import NetGraphAlgebraDefs.*

import java.io.{FileInputStream, ObjectInputStream}

object HelperFunctions {
  def load_graph(fileName: String, dir: String): Option[NetGraph] = {
    val fileInputStream: FileInputStream = new FileInputStream(s"$dir$fileName")
    val objectInputStream: ObjectInputStream = new ObjectInputStream(fileInputStream)
    val ng = objectInputStream.readObject.asInstanceOf[List[NetGraphComponent]]

    val graph = NetModelAlgebra(
      nodes = ng.collect { case node: NodeObject => node },
      edges = ng.collect { case edge: Action => edge }
    )
    objectInputStream.close()
    fileInputStream.close()
    graph
  }

}
