
import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._

object BldUtilitiesSJvm {

  lazy val `utilities-sjvm` = project.in(file("sjvm")).
    configure( commonSettings, noPublish ).
    settings(
      organization := "com.github.thebridsk",
      name := "utilities-sjvm",
      mainClass := None,

      libraryDependencies ++= sharedDeps.value,

      fork in Test := true,

    ).
    dependsOn(BldUtilitiesShared.sharedJVM % "test->test;compile->compile")

}
