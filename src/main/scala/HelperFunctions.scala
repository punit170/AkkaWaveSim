package AkkaPac

import NetGraphAlgebraDefs.{Action, NetGraph, NetGraphComponent, NetModelAlgebra, NodeObject}

import java.io.{FileInputStream, ObjectInputStream}

object HelperFunctions {
  def load_graph(fileName: String, dir: String, FS: String): Option[NetGraph] = {
    if (FS == "local" || FS == "file:///") then
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

    else
      throw new Exception("hadoopFS should be set to either on local path, hdfs localhost path or s3 bucket path")
  }

}
