ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

val scalaParCollVersion = "1.0.4"
val sfl4sVersion = "2.0.0-alpha5"
val logbackVersion = "1.2.10"
val typeSafeConfigVersion = "1.4.2"
val circeVersion = "0.14.1"
val netBuddyVersion = "1.14.4"
val guavaVersion = "31.1-jre"
val guavaAdapter2jGraphtVersion = "1.5.2"
val jGraphTlibVersion = "1.5.2"
val graphVizVersion = "0.18.1"
val catsVersion = "2.9.0"
val apacheCommonsVersion = "2.13.0"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
lazy val akkaVersion = "2.8.5"

lazy val root = (project in file("."))
  .settings(
    name := "AkkaWaveSim",
    Compile / unmanagedJars += baseDirectory.value / "lib" / "NetGameSim_thinJar.jar",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parallel-collections" % scalaParCollVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.typesafe" % "config" % typeSafeConfigVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.12" % Test,
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "net.bytebuddy" % "byte-buddy" % netBuddyVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scala-lang" %% "scala3-compiler" % "3.2.2",
      "com.google.guava" % "guava" % guavaVersion,
      "guru.nidi" % "graphviz-java" % graphVizVersion,
      "org.typelevel" %% "cats-core" % catsVersion,
      "commons-io" % "commons-io" % apacheCommonsVersion,
      "org.jgrapht" % "jgrapht-core" % jGraphTlibVersion,
      "org.jgrapht" % "jgrapht-guava" % guavaAdapter2jGraphtVersion)
  )
