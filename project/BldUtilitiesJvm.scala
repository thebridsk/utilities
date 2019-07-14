
import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._

object BldUtilitiesJvm {

  lazy val `utilities-jvm` = project.in(file("jvm")).
    configure( commonSettings, buildInfo("com.github.thebridsk.utilities.version", "VersionUtilities") ).
    disablePlugins(ScalaJSPlugin).
    settings(
      organization := "com.github.thebridsk",
      name := "utilities-jvm",
      mainClass := None,
      isScalaJSProject := false,
      libraryDependencies ++= utilitiesDeps.value,

      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.ManagedClasses,

      fork in Test := true,

      // include the macro classes and resources in the main jar
      mappings in (Compile, packageBin) ++= mappings.in(BldUtilitiesMacros.`utilities-macros`, Compile, packageBin).value,
      mappings in (Compile, packageSrc) ++= mappings.in(BldUtilitiesMacros.`utilities-macros`, Compile, packageSrc).value
    ).
    settings( buildInfoCommonSettings: _* ).
    dependsOn(BldUtilitiesMacros.`utilities-macros`)

}