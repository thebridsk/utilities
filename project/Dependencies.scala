
import sbt._

// Scala.js additions, see http://www.scala-js.org/doc/project/
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

// see http://www.scala-sbt.org/0.13/docs/Organizing-Build.html

object Dependencies {
  // version numbers

  lazy val verScalaVersion = "2.12.4"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq("2.11.8", verScalaVersion)

  lazy val vScalactic = "3.0.4"      // https://mvnrepository.com/artifact/org.scalactic/scalactic_2.12
  lazy val vScalatest = "3.0.4"      // http://mvnrepository.com/artifact/org.scalatest/scalatest_2.11
  lazy val vJunit = "4.12"           // http://mvnrepository.com/artifact/junit/junit

  lazy val vScalaArm = "2.0"         // http://mvnrepository.com/artifact/com.jsuereth/scala-arm_2.11
  lazy val vScallop = "3.1.1"        // http://mvnrepository.com/artifact/org.rogach/scallop_2.11


  // libraries


  val lScallop   = "org.rogach"    %%  "scallop"   % vScallop withSources()
  val lJunit = "junit"         %   "junit"     % vJunit  % "test" withSources()


  // projects

  // to use %%% you must be in a Def.setting
  // see https://github.com/vmunier/play-with-scalajs-example/issues/20
  // the use of the variable then needs to use bridgeScorerDeps.value

  val scalatestDeps = Def.setting(Seq(
      "org.scalatest" %%% "scalatest" % vScalatest % "test" withSources()
      ))

  val loggingDeps = Def.setting(scalatestDeps.value ++ Seq(
      "com.jsuereth" %% "scala-arm" % vScalaArm withSources(),
      "org.scalactic" %%% "scalactic" % vScalactic withSources()
      ))

  val sharedDeps = Def.setting(scalatestDeps.value ++ Seq(
      "org.scalactic" %%% "scalactic" % vScalactic withSources()
      ))

  val utilitiesDeps = Def.setting(scalatestDeps.value ++ Seq(
      lScallop,
      lJunit
      ))

}
