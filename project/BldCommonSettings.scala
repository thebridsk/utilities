
import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
//import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{crossProject => _, CrossType => _, _}
import org.scalajs.sbtplugin.ScalaJSCrossVersion
// import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
// import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

import BldVersion._
import MyReleaseVersion._
import XTimestamp._

/**
  * The idea comes from https://github.com/japgolly/scalajs-react/blob/master/project/Build.scala
  *
  * To use,
  *    val x = project.
  *                configure(commonSettings,noTests)
  */
object BldCommonSettings {

  /**
   * Add common settings to a project.
   *
   * Sets:
   *   scala version
   *   scalac options
   *   test options
   */
  def commonSettings: Project => Project =
    _.settings(versionSetting).settings(
      scalaVersion := verScalaVersion,
      crossScalaVersions := verCrossScalaVersions,
      scalacOptions := Seq(
        "-unchecked",
        "-deprecation",
        "-encoding",
        "utf8",
        "-feature" /* , "-Xlog-implicits" */
      ),
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      // libraryDependencies += scalaVersion(
      //   "org.scala-lang" % "scala-compiler" % _
      // ).value
      //  EclipseKeys.useProjectId := true,
    )

  /**
   * Add if project has no test cases
   */
  def noTests(js: Boolean): Project => Project = { proj =>
    val p = proj.settings(
      test in Test := {},
      testOnly in Test := {},
      testQuick in Test := {}
    )
    if (js) {
      p.settings(
        fastOptJS in Test := Attributed(
          artifactPath.in(fastOptJS).in(Test).value
        )(AttributeMap.empty),
        fullOptJS in Test := Attributed(
          artifactPath.in(fullOptJS).in(Test).value
        )(AttributeMap.empty),
      )
    } else {
      p
    }
  }

  /**
   * Add if project should not be published
   */
  def noPublish: Project => Project =
    _.settings(
      publishTo := Some(
        Resolver
          .file("Unused transient repository", target.value / "fakepublish")
      ),
      publishArtifact := false,
      publishLocal := {},
      // publishLocalSigned := {}, // doesn't work
      // publishSigned := {}, // doesn't work
      packagedArtifacts := Map.empty // doesn't work - https://github.com/sbt/sbt-pgp/issues/42
    )

  lazy val buildInfoCommonSettings = Seq(

    // this replaces
    //
    //     buildInfoOptions += BuildInfoOption.BuildTime
    //
    // This uses a constant timestamp if it is a snapshot build
    // to mitigate a long build time.

    buildInfoKeys ++= Seq[BuildInfoKey](
      BuildInfoKey.action( "builtAtString" ) {
          string(isSnapshotVersion)
      },
      BuildInfoKey.action( "builtAtMillis" ) {
          millis(isSnapshotVersion)
      }
    )
  )

  def buildInfo( pack: String, cls: String ): Project => Project =
    _.enablePlugins(BuildInfoPlugin).
      settings(
        buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
        buildInfoPackage := pack,
        buildInfoObject := cls,
        buildInfoUsePackageAsPath := true,
    //    buildInfoOptions += BuildInfoOption.BuildTime,
        buildInfoOptions += BuildInfoOption.ToJson,
      ).
      settings(buildInfoCommonSettings)

  /**
    * Add command aliases
    *
    * Usage:
    *
    * val x = project.
    *           configure( addCommandAlias( "cmd" -> "test:compile", "cc" -> ";clean;compile" ))
    *
    */
  def addCommandAliases(m: (String, String)*)(proj: Project) = {
    val s = m.map(p => addCommandAlias(p._1, p._2)).reduce(_ ++ _)
    proj.settings(s: _*)
  }

}
