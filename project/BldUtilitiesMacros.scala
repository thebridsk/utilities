
import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._

object BldUtilitiesMacros {

  lazy val `utilities-macros` = project.in(file("macros")).
    disablePlugins(ScalaJSPlugin).
    configure( commonSettings, noPublish ).
    settings(
      organization := "com.github.thebridsk",
      name := "utilities-macros",
      mainClass := None,
      libraryDependencies ++= loggingDeps.value,

      fork in Test := true,

      publish := {},
      publishLocal := {}
    )

}
