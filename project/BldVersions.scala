

object BldVersion {

  lazy val verScalaVersion = "2.12.8"
  lazy val verScalaMajorMinor = {
    val i = verScalaVersion.indexOf('.')
    val i2 = verScalaVersion.indexOf('.', i+1)
    verScalaVersion.substring(0, i2)
  }

  lazy val verCrossScalaVersions = Seq("2.12.8", verScalaVersion)

  lazy val vScalactic = "3.0.8"      // https://mvnrepository.com/artifact/org.scalactic/scalactic_2.12
  lazy val vScalatest = "3.0.8"      // http://mvnrepository.com/artifact/org.scalatest/scalatest_2.11
  lazy val vJunit = "4.12"           // http://mvnrepository.com/artifact/junit/junit

  lazy val vScalaArm = "2.0"         // http://mvnrepository.com/artifact/com.jsuereth/scala-arm_2.11
  lazy val vScallop = "3.3.1"        // http://mvnrepository.com/artifact/org.rogach/scallop_2.11

}
