
import Dependencies._
import MyEclipseTransformers._
import MyReleaseVersion._

enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)

//
// Debugging deprecation and feature warnings
//
// Through the sbt console...
//
//    reload plugins
//    set scalacOptions ++= Seq( "-unchecked", "-deprecation", "-feature" )
//    session save
//    reload return

EclipseKeys.skipParents in ThisBuild := false

lazy val commonSettings = versionSetting ++ Seq(
  scalaVersion  := verScalaVersion,
  crossScalaVersions := verCrossScalaVersions,
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature" /* , "-Xlog-implicits" */),
  EclipseKeys.withSource := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
//  EclipseKeys.useProjectId := true
)

lazy val utilities = project.in(file(".")).
  settings( commonSettings: _* ).
  settings(
    organization := "com.example",
    name := "utilities",
    publish := {},
    publishLocal := {}
  ).
  aggregate(`utilities-macros`, sharedJS, sharedJVM, `utilities-js`, `utilities-jvm`, `utilities-sjvm` )
  

lazy val `utilities-jvm` = project.in(file("jvm")).
  enablePlugins(BuildInfoPlugin).
  disablePlugins(ScalaJSPlugin).
  settings( commonSettings: _* ).
  settings(
    organization := "com.example",
    name := "utilities-jvm",
    mainClass := None,
    isScalaJSProject := false,
    libraryDependencies ++= utilitiesDeps.value,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.example.utilities.version",
    buildInfoObject := "VersionUtilities",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,
    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.ManagedClasses,

    fork in Test := true,

    // include the macro classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings.in(`utilities-macros`, Compile, packageBin).value,
    mappings in (Compile, packageSrc) ++= mappings.in(`utilities-macros`, Compile, packageSrc).value
  ).
  dependsOn(`utilities-macros`)

lazy val `utilities-macros` = project.in(file("macros")).
  disablePlugins(ScalaJSPlugin).
  settings( commonSettings: _* ).
  settings(
    organization := "com.example",
    name := "utilities-macros",
    mainClass := None,
    isScalaJSProject := false,
    libraryDependencies ++= loggingDeps.value,
    libraryDependencies += scalaVersion("org.scala-lang" % "scala-compiler" % _).value,

    fork in Test := true,

    publish := {},
    publishLocal := {}
  )

lazy val `utilities-js` = project.in(file("js")).
  enablePlugins(ScalaJSPlugin).
  settings( commonSettings: _* ).
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

    publish := {},
    publishLocal := {}
  ).
  dependsOn(sharedJS % "test->test;compile->compile")

lazy val `utilities-sjvm` = project.in(file("sjvm")).
  settings( commonSettings: _* ).
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
    
    publish := {},
    publishLocal := {}
  ).
  dependsOn(sharedJVM % "test->test;compile->compile")

lazy val `utilities-shared` = crossProject.in(file("shared")).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings: _*).
  settings(
    name := "utilities-shared",
    resolvers += Resolver.bintrayRepo("scalaz", "releases"),

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "utils.version",
    buildInfoObject := "VersionShared",
    buildInfoUsePackageAsPath := true,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    libraryDependencies ++= sharedDeps.value,

//    EclipseKeys.useProjectId := true,
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


val rootfilter = ScopeFilter(
     inAggregates(utilities, includeRoot = false)
   )


lazy val Distribution = config("distribution") describedAs("tasks for creating a distribution.")

val mydist = taskKey[Unit]("Make a build for distribution") in Distribution

val travis = taskKey[Unit]("The build done in Travis CI") in Distribution

mydist := Def.sequential( 
                clean.all(rootfilter),
                (test in Test).all(rootfilter), 
                packageBin in Compile in `utilities-jvm`
          ).value

travis := Def.sequential( 
                clean.all(rootfilter),
                (test in Test).all(rootfilter), 
                packageBin in Compile in `utilities-jvm`
          ).value

releaseUseGlobalVersion := false

//
// need to update release tag and comment
//
releaseTagName := "v"+git.baseVersion.value

releaseTagComment := s"Releasing ${git.baseVersion.value}"

releaseCommitMessage := s"Setting version to ${git.baseVersion.value}"

import ReleaseTransformations.{ setReleaseVersion=>_, setNextVersion=>_, _ }

lazy val releaseCheck = { st: State =>
  println("Checking for release")
//  sys.error("failed check for release")
  st
}

val publishRelease = ReleaseStep(
  check  = releaseCheck,                                       // upfront check
  action = releaseStepTaskAggregated(mydist in Distribution in utilities) // publish release notes
)

import MyReleaseVersion._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  gitMakeReleaseBranch,
  inquireVersions,                        // : ReleaseStep
//  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  recalculateVersion,                     // : ReleaseStep
  publishRelease,                         // : ReleaseStep, custom
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
//  gitPushReleaseBranch
//  gitMergeReleaseMaster,
//  recalculateVersion,                     // : ReleaseStep
//  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)
  