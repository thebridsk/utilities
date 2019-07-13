
import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._

object BldUtilitiesSJvm {

  lazy val `utilities-sjvm` = project.in(file("sjvm")).
    configure( commonSettings, noPublish ).
    settings(
      organization := "com.example",
      name := "utilities-sjvm",
      mainClass := None,
      isScalaJSProject := false,

      EclipseKeys.classpathTransformerFactories ++= Seq(
  //      MyEclipseTransformers.replaceRelativePath("/utilities-shared", "/utilities-sharedJVM")
      ),

      libraryDependencies ++= sharedDeps.value,
      libraryDependencies += scalaVersion("org.scala-lang" % "scala-compiler" % _).value,

      fork in Test := true,

    ).
    dependsOn(BldUtilitiesShared.sharedJVM % "test->test;compile->compile")

}