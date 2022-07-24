ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "sparkstreaming-kafka"
  )

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "3.2.0",
  "org.apache.spark" %% "spark-core" % "3.3.0",
  "org.apache.spark" %% "spark-streaming" % "3.3.0" % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.2.1",
  "org.apache.spark" %% "spark-sql" % "3.3.0" % "provided",
  "org.apache.hbase" % "hbase-shaded-client" % "2.4.12"
)