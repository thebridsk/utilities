
// To check the versions of the plugins, issue the following command:
//
//    sbt "reload plugins" dependencyUpdates
//

name := "project-utilities"

scalaVersion := "2.12.8"

val vLog4j = "1.7.26"              // https://github.com/qos-ch/slf4j
val vJGit = "5.4.0.201906121030-r" // https://github.com/eclipse/jgit

val vCrossProject = "0.6.1"        // https://github.com/portable-scala/sbt-crossproject
val vPlatformDeps = "1.0.0"        // https://github.com/portable-scala/sbt-platform-deps
val vScalaJSDefault = "0.6.28"     // http://www.scala-js.org/
val vSbtAssembly = "0.14.10"        // https://github.com/sbt/sbt-assembly
val vSbtGit = "1.0.0"              // https://github.com/sbt/sbt-git
val vSbtSCoverage = "1.5.1"        // https://github.com/scoverage/sbt-scoverage
val vSbtBuildInfo = "0.9.0"        // https://github.com/sbt/sbt-buildinfo
val vSbtRelease = "1.0.11"         // https://github.com/sbt/sbt-release
val vSbtEclipse = "5.2.4"          // https://github.com/typesafehub/sbteclipse
val vSbtDependencyGraph = "0.9.2"  // https://github.com/jrudolph/sbt-dependency-graph
val vSbtUpdates = "0.4.2"          // https://github.com/rtimush/sbt-updates
val vSbtEnsime = "2.6.1"           // https://github.com/ensime/ensime-sbt
val vSbtScalaFmt="2.0.2"           // https://github.com/scalameta/sbt-scalafmt
val vBloop = "1.3.2"               // https://github.com/scalacenter/bloop

val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse(vScalaJSDefault)

// The following is needed to get rid of the message
//   SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
// when sbt is started.

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % vLog4j

// Unfortunately this causes an exception to be logged to the console from sbt-git plugin
// because it can't find git.
// update jgit plugin to avoid exception

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % vJGit
     exclude("javax.jms", "jms")
     exclude("com.sun.jdmk", "jmxtools")
     exclude("com.sun.jmx", "jmxri")
)

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % vCrossProject withSources())
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion withSources())

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % vSbtAssembly withSources())
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % vSbtGit withSources())
addSbtPlugin("org.scoverage" % "sbt-scoverage" % vSbtSCoverage withSources())
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % vSbtBuildInfo withSources())
addSbtPlugin("com.github.gseitz" % "sbt-release" % vSbtRelease withSources())
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % vSbtEclipse withSources())
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % vSbtUpdates withSources())
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % vSbtDependencyGraph withSources())  // must be updated for sbt 1.0

addSbtPlugin("org.ensime" % "sbt-ensime" % vSbtEnsime withSources())
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % vSbtScalaFmt withSources())
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % vBloop withSources())

scalacOptions ++= Seq( "-unchecked", "-deprecation" )

scalacOptions ++= Seq( "-unchecked", "-feature", "-deprecation" )


EclipseKeys.classpathTransformerFactories ++= Seq(
  MyProjectEclipseTransformers.addProjectFolderToClasspath()
)
