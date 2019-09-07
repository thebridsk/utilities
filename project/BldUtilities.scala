
import sbt._
import Keys._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitBranchPrompt

import BldDependencies._
import BldCommonSettings._

import MyReleaseVersion._

import sbtcrossproject.{crossProject, CrossType}

object BldUtilities {

  lazy val Distribution = config("distribution") describedAs ("tasks for creating a distribution.")

  val mydist = taskKey[Unit]("Make a build for distribution") in Distribution

  val travis = taskKey[Unit]("The build done in Travis CI") in Distribution

  import ReleaseTransformations.{setReleaseVersion => _, setNextVersion => _, _}

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

      mydist := Def
        .sequential(
          clean.all(rootfilter),
          (test in Test).all(rootfilter),
          packageBin in Compile in BldUtilitiesJvm.`utilities-jvm`
        )
        .value,

      travis := Def.sequential(
          clean.all(rootfilter),
          (test in Test).all(rootfilter),
          packageBin in Compile in BldUtilitiesJvm.`utilities-jvm`
        )
        .value,

      releaseUseGlobalVersion := false,

      //
      // need to update release tag and comment
      //
      releaseTagName := "v" + git.baseVersion.value,

      releaseTagComment := s"Releasing ${git.baseVersion.value}",

      releaseCommitMessage := s"Setting version to ${git.baseVersion.value}",

      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies, // : ReleaseStep
        gitMakeReleaseBranch,
        inquireVersions, // : ReleaseStep
      //  runTest,                                // : ReleaseStep
        setReleaseVersion, // : ReleaseStep
        commitReleaseVersion, // : ReleaseStep, performs the initial git checks
        tagRelease, // : ReleaseStep
        recalculateVersion, // : ReleaseStep
        publishRelease, // : ReleaseStep, custom
        setNextVersion, // : ReleaseStep
        commitNextVersion // : ReleaseStep
      //  gitPushReleaseBranch
      //  gitMergeReleaseMaster,
      //  recalculateVersion,                     // : ReleaseStep
      //  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
      )

    )


  lazy val rootfilter = ScopeFilter(
    inAggregates(BldUtilities.utilities, includeRoot = true)
  )

}