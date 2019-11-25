import sbt.dsl._
import NativePackagerKeys._


Common.appSettings

name := """play-silhouette"""

version := "1.0-SNAPSHOT"


scalaVersion := "2.12.3"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Common.commonDependencies ++ Common.webDependencies

routesGenerator := InjectedRoutesGenerator

traceLevel in run := 0



lazy val api = (project in file("modules/api")).enablePlugins(PlayScala)

lazy val root = (project in file("."))
                .enablePlugins(PlayScala, SbtWeb)
                .aggregate(api)
                .dependsOn(api)

scalacOptions += "-Ypartial-unification"

WebKeys.webTarget := target.value / "scala-web"
