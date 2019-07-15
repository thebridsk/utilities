
import sbt._
import Keys._

import sbtcrossproject.{crossProject, CrossType}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._

import BldDependencies._
import BldCommonSettings._
import BldVersion._

object BldUtilitiesShared {

  lazy val `utilities-shared` = crossProject(JSPlatform, JVMPlatform).in(file("shared")).
    configure( commonSettings, noPublish, noTests(false), buildInfo("utils.version", "VersionShared") ).
    settings(
      name := "utilities-shared",
      resolvers += Resolver.bintrayRepo("scalaz", "releases"),

      libraryDependencies ++= sharedDeps.value,

      // EclipseKeys.useProjectId := true,
      EclipseKeys.classpathTransformerFactories ++= Seq(
        MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-main-scala", "shared-src-main-scala"),
        MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-test-scala", "shared-src-test-scala"),
        MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-main-scala-"+verScalaMajorMinor, "shared-src-main-scala-"+verScalaMajorMinor),
        MyEclipseTransformers.fixLinkedNameFromClasspath("-shared-shared-src-test-scala-"+verScalaMajorMinor, "shared-src-test-scala-"+verScalaMajorMinor)
      ),
      EclipseKeys.projectTransformerFactories ++= Seq(
        MyEclipseTransformers.fixLinkName("-shared-shared-src-main-scala", "shared-src-main-scala"),
        MyEclipseTransformers.fixLinkName("-shared-shared-src-test-scala", "shared-src-test-scala"),
        MyEclipseTransformers.fixLinkName("-shared-shared-src-main-scala-"+verScalaMajorMinor, "shared-src-main-scala-"+verScalaMajorMinor),
        MyEclipseTransformers.fixLinkName("-shared-shared-src-test-scala-"+verScalaMajorMinor, "shared-src-test-scala-"+verScalaMajorMinor)
      )

    ).
    settings( buildInfoCommonSettings: _* ).
    jvmSettings(

    ).
    jsSettings(

      // This gets rid of the jetty check which is required for the sbt runtime
      // not the application
      //   [info]   org.eclipse.jetty:jetty-server:phantom-js-jetty    : 8.1.16.v20140903 -> 8.1.19.v20160209 -> 9.4.0.M0
      //   [info]   org.eclipse.jetty:jetty-websocket:phantom-js-jetty : 8.1.16.v20140903 -> 8.1.19.v20160209
  //    dependencyUpdatesExclusions := moduleFilter(organization = "org.eclipse.jetty")
      dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty")
    )

  lazy val sharedJS: Project = `utilities-shared`.js
  lazy val sharedJVM = `utilities-shared`.jvm

}