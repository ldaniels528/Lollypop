import sbt.Keys._
import sbt._

import scala.language.postfixOps

val scalaVersion_2_13 = "2.13.10"
val scalaVersion_3_00 = "3.0.1"

val appVersion = "0.1.4"
val pluginVersion = "1.0.0"
val scalaAppVersion = scalaVersion_2_13

val akkaVersion = "2.8.2"
val akkaHttpVersion = "10.5.2"
val awsKinesisClientVersion = "1.14.10"
val awsSDKVersion = "1.11.946"
val commonsIOVersion = "2.11.0"
val jekaVersion = "0.10.20"
val liftJsonVersion = "3.4.3"
val log4jVersion = "1.2.17"
val scalaJsIoVersion = "0.7.0"
val scalaTestVersion = "3.3.0-SNAP3"
val slf4jVersion = "2.0.5"
val snappyJavaVersion = "1.1.9.1"

lazy val testDependencies = Seq(
  libraryDependencies ++= Seq(
    "log4j" % "log4j" % log4jVersion,
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "org.slf4j" % "slf4j-log4j12" % slf4jVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  ))

/////////////////////////////////////////////////////////////////////////////////
//      Root Project - builds all artifacts
/////////////////////////////////////////////////////////////////////////////////

lazy val root = (project in file("./app")).
  aggregate(core, jdbc_driver).
  dependsOn(core, jdbc_driver).
  settings(testDependencies: _*).
  settings(
    name := "qwery-platform",
    organization := "com.qwery",
    description := "Qwery Platform",
    version := appVersion,
    scalaVersion := scalaAppVersion,
    Compile / console / scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    Compile / doc / scalacOptions += "-no-link-warnings",
    autoCompilerPlugins := true
  )

/////////////////////////////////////////////////////////////////////////////////
//      Core Project
/////////////////////////////////////////////////////////////////////////////////

/**
 * @example sbt "project core" test
 */
lazy val core = (project in file("./app/core")).
  settings(testDependencies: _*).
  settings(
    name := "core",
    organization := "com.qwery",
    description := "Qwery core language, run-time and utilities",
    version := appVersion,
    scalaVersion := scalaAppVersion,
    Compile / console / scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    Compile / doc / scalacOptions += "-no-link-warnings",
    autoCompilerPlugins := true,
    assembly / mainClass := Some("com.qwery.repl.QweryCLI"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case PathList("org", "apache", _*) => MergeStrategy.first
      case PathList("akka-http-version.conf") => MergeStrategy.concat
      case PathList("reference.conf") => MergeStrategy.concat
      case PathList("version.conf") => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    assembly / test := {},
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "commons-io" % "commons-io" % commonsIOVersion,
      "commons-codec" % "commons-codec" % "1.15",
      "dev.jeka" % "jeka-core" % jekaVersion,
      "org.apache.commons" % "commons-math3" % "3.6.1",
      "org.apache.httpcomponents.client5" % "httpclient5" % "5.2.1",
      "org.apache.httpcomponents.client5" % "httpclient5-fluent" % "5.2.1",
      "org.commonmark" % "commonmark" % "0.21.0",
      "org.jfree" % "jfreechart" % "1.5.4",
      "org.ow2.asm" % "asm" % "9.4",
      "org.scala-lang" % "scala-reflect" % scalaAppVersion,
      "org.xerial.snappy" % "snappy-java" % snappyJavaVersion
    ))

/////////////////////////////////////////////////////////////////////////////////
//      Database Client Project
/////////////////////////////////////////////////////////////////////////////////

/**
 * @example sbt "project jdbc_driver" test
 */
lazy val jdbc_driver = (project in file("./app/jdbc-driver")).
  dependsOn(core).
  settings(testDependencies: _*).
  settings(
    name := "jdbc-driver",
    organization := "com.qwery",
    description := "Qwery Database JDBC Driver",
    version := appVersion,
    scalaVersion := scalaAppVersion,
    Compile / console / scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    Compile / doc / scalacOptions += "-no-link-warnings",
    autoCompilerPlugins := true,
    assembly / mainClass := Some("com.qwery.database.jdbc.QweryNetworkClient"),
    assembly / test := {},
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case PathList("org", "apache", _*) => MergeStrategy.first
      case PathList("akka-http-version.conf") => MergeStrategy.concat
      case PathList("reference.conf") => MergeStrategy.concat
      case PathList("version.conf") => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    libraryDependencies ++= Seq(

    ))

/////////////////////////////////////////////////////////////////////////////////
//      Contrib Project
/////////////////////////////////////////////////////////////////////////////////

lazy val contrib = (project in file("./contrib")).
  aggregate(root, examples).
  dependsOn(root, examples).
  settings(testDependencies: _*).
  settings(
    name := "qwery-demos",
    organization := "com.qwery",
    description := "Qwery Demos",
    version := appVersion,
    scalaVersion := scalaAppVersion,
    Compile / console / scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    Compile / doc / scalacOptions += "-no-link-warnings",
    autoCompilerPlugins := true
  )

/////////////////////////////////////////////////////////////////////////////////
//      Examples Project
/////////////////////////////////////////////////////////////////////////////////

/**
 * @example sbt "project examples" test
 */
lazy val examples = (project in file("./contrib/examples")).
  dependsOn(jdbc_driver).
  settings(testDependencies: _*).
  settings(
    name := "examples",
    organization := "com.qwery.examples",
    description := "Qwery Examples",
    version := appVersion,
    scalaVersion := scalaAppVersion,
    Compile / console / scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    Compile / doc / scalacOptions += "-no-link-warnings",
    autoCompilerPlugins := true)

// loads the Scalajs-io root project at sbt startup
onLoad in Global := (Command.process("project root", _: State)) compose (onLoad in Global).value
