

object BldVersion {

  lazy val verScalaVersion = "2.12.8"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq("2.12.8", verScalaVersion)

  lazy val vScalactic = "3.0.8"      // https://github.com/scalatest/scalatest
  lazy val vScalatest = "3.0.8"      // https://github.com/scalatest/scalatest
  lazy val vJunit = "4.12"           // https://github.com/junit-team/junit4

  lazy val vScalaArm = "2.0"         // https://github.com/jsuereth/scala-arm
  lazy val vScallop = "3.3.1"        // https://github.com/scallop/scallop

}
