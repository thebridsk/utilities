
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

  val testClass = inputKey[Unit]("Run specified test class")

  def walkTree(file: File): Iterable[File] = {
    // println(s"walkTree file $file")
    val children = new Iterable[File] {
      def iterator = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
    }
    Seq(file) ++: children.flatMap(walkTree(_))
  }

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
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDG"),
      testClass in Test := (Def.inputTaskDyn {
        import complete.DefaultParsers._
        val args: Seq[String] = spaceDelimited("<arg>").parsed
        val testdir = (scalaSource in Test).value
        val lentestdir = testdir.toString.length
        // println(s"testClass testdir $testdir")
        val (options,atests) = args.foldLeft( (List[String](), List[String]()) ) { (ac,s) =>
          // println(s"testClass processing $s: $ac")
          if (s.charAt(0)=='-') (ac._1:::List(s), ac._2)
          else {
            // this needs to be resolved into a classname
            val gscalaclass = s"${s.replace('.','/')}.scala"
            // println(s"testClass scalaclass=$gscalaclass")
            val gf = GlobFilter(gscalaclass)
            val wtests = walkTree(testdir).filter(_.toString.length>lentestdir).map(_.toString.substring(lentestdir+1).replace('\\','/')).filter(gf.accept(_)).map{ f =>
              f.substring(0, f.length-6)
            }.toList
            // println(s"testClass found $wtests")
            (ac._1,ac._2:::wtests)
          }
        }
        if (atests.isEmpty) {
          (Def.task {
            val log = streams.value.log
            log.error("Test class must be specified")
          })
        } else {
          val ra = s""" org.scalatest.tools.Runner -oD ${(atests.map( t => s"-s ${t.replace('/','.')}"):::options).mkString(" ")}"""
          // println(s"testClass running=${ra}")
          (Def.taskDyn {
            (runMain in Test).toTask( ra )
          })
        }

      }).evaluated,
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
