
import sbt._
import Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSCrossVersion
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}

import BldVersion._

object BldDependencies {

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
      "org.rogach"    %%  "scallop"   % vScallop withSources(),
      "junit"         %   "junit"     % vJunit  % "test" withSources()
      ))

}
