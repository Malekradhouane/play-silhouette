import play.sbt.PlayImport._
import sbt.Keys._
import sbt._
import com.typesafe.sbt.SbtScalariform._

object Common {

  def appName = "play-silhouette"

  def settings(theName: String) = Seq(
    name := theName,
    scalaVersion := "2.12.3",
    scalacOptions ++= Seq(
      "-feature", "-deprecation", "-unchecked", "-language:reflectiveCalls"
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      Resolver.jcenterRepo
    )
  ) ++ scalariformSettings
  val appSettings = settings(appName)
  def moduleSettings(module: String) = settings(module) ++: Seq(
    javaOptions in Test += s"-Dconfig.resource=application.conf"
  )

  val commonDependencies = Seq(
    ws,
    guice,
    "com.typesafe.play" %% "play-json" % "2.6.1",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.12.6-play26",
    "org.mockito" % "mockito-core" % "1.10.19" % "test"


  )

  val webDependencies = Seq (
    "org.webjars" %% "webjars-play" % "2.6.0",
    "org.webjars" %  "bootstrap" % "3.1.1-2",
    "org.webjars" %  "jquery" % "1.8.3"

  )

  val authenticationDependencies = Seq (
    "net.codingwell" %% "scala-guice" % "4.2.1",
    "com.iheart" %% "ficus" % "1.4.2",
    "com.mohiva" %% "play-silhouette" % "5.0.0",
    "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.0",
    "com.mohiva" %% "play-silhouette-persistence" % "5.0.0",
    "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.0",
    "com.mohiva" %% "play-silhouette-testkit" % "5.0.0" % "test"
  )
}
