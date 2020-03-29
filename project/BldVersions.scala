

object BldVersion {

  lazy val verScalaVersion = "2.13.1"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq("2.13.1", verScalaVersion)

  lazy val vScalactic = "3.1.1"      // https://github.com/scalatest/scalatest
  lazy val vScalatest = "3.1.1"      // https://github.com/scalatest/scalatest
  lazy val vJunit = "4.13"           // https://github.com/junit-team/junit4

  lazy val vScallop = "3.4.0"        // https://github.com/scallop/scallop

}
