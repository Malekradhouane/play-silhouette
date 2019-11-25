Common.moduleSettings("""api""")

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  Resolver.sonatypeRepo("snapshots")
)

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Common.commonDependencies ++: Common.webDependencies ++: Common.authenticationDependencies ++: Seq(
  filters,
  "commons-io" % "commons-io" % "2.4",
  "com.google.inject" % "guice" % "4.0",
  "javax.inject" % "javax.inject" % "1",
  "org.freemarker" % "freemarker" % "2.3.27-incubating",
  "org.typelevel" %% "cats-core" % "1.2.0",
  "com.cloudinary" % "cloudinary-core" % "1.22.1"

)

scalacOptions += "-Ypartial-unification"
