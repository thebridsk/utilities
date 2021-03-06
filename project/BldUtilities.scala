
import sbt._
import Keys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitBranchPrompt

import BldVersion._
import BldDependencies._
import BldCommonSettings._

import MyReleaseVersion._

import scalafix.sbt.ScalafixPlugin.autoImport._

import sbtcrossproject.{crossProject, CrossType}

object BldUtilities {

  lazy val Distribution = config("distribution") describedAs ("tasks for creating a distribution.")

  val myclean = taskKey[Unit]("clean") in Distribution

  val mydistnoclean = taskKey[Unit]("Make a build for distribution, no clean") in Distribution

  val mydist = taskKey[Unit]("Make a build for distribution") in Distribution

  val travis = taskKey[Unit]("The build done in Travis CI") in Distribution

  import ReleaseTransformations.{setReleaseVersion => _, setNextVersion => _, _}

  val setSemanticDB = Command.command(
    "setSemanticDB",
    "turn on setSemanticDB",
    "turn on setSemanticDB"
  ) { state: State =>
    val extracted = Project extract state
    import extracted._
    println("Turning on SemanticDB")
    appendWithoutSession(
      semanticdbEnabled in ThisBuild := true,
      state
    )
  }

  def init = inThisBuild(
    List(
      scalaVersion := verScalaVersion,
      scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(verScalaVersion),
      semanticdbEnabled := false,
      semanticdbVersion := scalafixSemanticdb.revision,

      Global / excludeLintKeys ++= Set(
        releaseProcess in BldUtilities.utilities,
        releaseUseGlobalVersion in BldUtilities.utilities,
        suppressSbtShellNotification in BldUtilities.utilities
      )

    )
  )

  val setOptimize = Command.command(
    "setOptimize",
    "turn on scalac optimization for all projects",
    "turn on scalac optimization for all projects"
  )( turnOnOptimize _)

  def turnOnOptimize( state: State ) = {
    val extracted = Project extract state
    import extracted._
    println("Turning on optimization in all projects")
    //append returns state with updated Foo
    appendWithoutSession(
      structure.allProjectRefs.map{ p =>
        println(s"  Turning on in {${p.build}}${p.project}")
        scalacOptions in p ++= Seq(
          "-opt:l:inline",
          "-opt-inline-from:**"
        )
      },
      state
    )
  }

  implicit class WrapState( val state: State ) extends AnyVal {
    def run[T]( key: TaskKey[T] ) = {
      releaseStepTask(key)(state)
    }
    def run( command: String ) = {
      releaseStepCommandAndRemaining(command)(state)
    }
  }

  val updateCheck = Command.command(
    "updateCheck",
    "Check for updates",
    "Check for updates"
  ) { state =>
    state
      .run(dependencyUpdates)
      .run("reload plugins")
      .run(dependencyUpdates)
      // .run("reload return")
    state
  }

  lazy val releaseOptimize = ReleaseStep(
    action = turnOnOptimize
  )

  lazy val releaseCheck = { st: State =>
    println("Checking for release")
  //  sys.error("failed check for release")
    st
  }

  val publishRelease = ReleaseStep(
    check = releaseCheck, // upfront check
    action = releaseStepTaskAggregated(mydist in Distribution in utilities) // publish release notes
  )

  lazy val utilities: Project = project.in(file(".")).
    configure( commonSettings, noPublish, buildInfo("com.github.thebridsk.utilities.version", "VersionUtilities") ).
    enablePlugins(GitVersioning, GitBranchPrompt).
    settings(
      organization := "com.github.thebridsk",
      name := "utilities",
      mainClass := None,
    ).
    aggregate(
      BldUtilitiesMacros.`utilities-macros`,
      BldUtilitiesShared.sharedJS,
      BldUtilitiesShared.sharedJVM,
      BldUtilitiesJs.`utilities-js`,
      BldUtilitiesJvm.`utilities-jvm`,
      BldUtilitiesSJvm.`utilities-sjvm`
    )
    .settings(

      ReleasePlugin.extraReleaseCommands,

      commands ++= Seq( setOptimize, updateCheck, releaseWithDefaults, setSemanticDB ),

      mydistnoclean := {
        val x = (test in Test).all(rootfilter).value
        val y = (packageBin in Compile in BldUtilitiesJvm.`utilities-jvm`).value
      },

      mydist := Def
        .sequential(
          myclean,
          mydistnoclean
        )
        .value,

      myclean := {
        val x = clean.all(rootfilter).value
      },

      travis := Def.sequential(
          clean.all(rootfilter),
          mydistnoclean
        )
        .value,

      releaseUseGlobalVersion := false,

      //
      // need to update release tag and comment
      //
      releaseTagName := getTagFromVersion( git.baseVersion.value ),

      releaseTagComment := s"Releasing ${git.baseVersion.value}",

      releaseCommitMessage := s"Setting version to ${git.baseVersion.value}",
      releaseNextCommitMessage := s"Setting version to ${git.baseVersion.value}",

      // This release process will only work if the command "release with-defaults" or
      // "myrelease-with-defaults" is used.
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        gitMakeReleaseBranch,
        inquireVersions,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        recalculateVersion,
        releaseOptimize,
        publishRelease,  // runs a clean build and test
        setNextVersion,
        commitNextVersion,
        gitPushReleaseBranch,
        gitPushReleaseTag,
      )

    )


  lazy val rootfilter = ScopeFilter(
    inAggregates(BldUtilities.utilities, includeRoot = true)
  )

}
