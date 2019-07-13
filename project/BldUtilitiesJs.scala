
import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._

object BldUtilitiesJs {

  lazy val `utilities-js` = project.in(file("js")).
    configure( commonSettings, noPublish ).
    enablePlugins(ScalaJSPlugin).
    settings(
      organization := "com.example",
      name := "utilities-js",
      mainClass := None,
      isScalaJSProject := true,

      parallelExecution in Test := false,

      EclipseKeys.classpathTransformerFactories ++= Seq(
  //      MyEclipseTransformers.replaceRelativePath("/utilities-shared", "/utilities-sharedJS")
      ),

      libraryDependencies ++= sharedDeps.value,
      libraryDependencies += scalaVersion("org.scala-lang" % "scala-compiler" % _).value,

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
  //    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty"),
      dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty"),

    ).
    dependsOn(BldUtilitiesShared.sharedJS % "test->test;compile->compile")

}