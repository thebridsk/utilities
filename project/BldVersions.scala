

object BldVersion {

  lazy val verScalaVersion = "2.13.3"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq(verScalaVersion)

  lazy val vScalactic = "3.2.2"      // https://github.com/scalatest/scalatest
  lazy val vScalatest = "3.2.2"      // https://github.com/scalatest/scalatest
  lazy val vJunit = "4.13.1"         // https://github.com/junit-team/junit4

  lazy val vScallop = "3.5.1"        // https://github.com/scallop/scallop

}
