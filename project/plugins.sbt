logLevel := Level.Warn

resolvers ++= Seq(
  "maven" at "http://repo1.maven.org/maven2",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  Classpaths.sbtPluginReleases,
  "bintray-sbt-plugin-releases" at "http://dl.bintray.com/content/sbt/sbt-plugin-releases"
)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.3.15")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.8-M8")
addSbtPlugin("com.heroku" % "sbt-heroku" % "1.0.1")



// DotEnv Plugin
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "1.1.36")
