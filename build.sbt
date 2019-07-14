
//
// Debugging deprecation and feature warnings
//
// Through the sbt console...
//
//    reload plugins
//    set scalacOptions ++= Seq( "-unchecked", "-deprecation", "-feature" )
//    session save
//    reload return

lazy val utilities = BldUtilities.utilities

lazy val `utilities-jvm` = BldUtilitiesJvm.`utilities-jvm`

lazy val `utilities-macros` = BldUtilitiesMacros.`utilities-macros`

lazy val `utilities-js` = BldUtilitiesJs.`utilities-js`

lazy val `utilities-sjvm` = BldUtilitiesSJvm.`utilities-sjvm`

lazy val sharedJS = BldUtilitiesShared.sharedJS
lazy val sharedJVM = BldUtilitiesShared.sharedJVM
