package com.github.thebridsk.utilities.main.test

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest._
import com.github.thebridsk.utilities.logging.Config
import com.github.thebridsk.utilities.classpath.ClassPath
import java.util.logging.LogManager
import java.util.logging.Logger
import com.github.thebridsk.utilities.logging.FileHandler
import com.github.thebridsk.utilities.logging.FileFormatter
import java.util.logging.Level
import com.github.thebridsk.utilities.logging.RedirectOutput

object TestStartLogging {

  val testlog = com.github.thebridsk.utilities.logging.Logger[TestStartLogging]

  private var loggingInitialized = false

  val logFilePrefix = "UseLogFilePrefix"
  val logFilePrefixDefault = "logs/unittest"

  def getProp( name: String, default: String ) = {
    sys.props.get(name) match {
      case Some(s) => s
      case None => sys.env.get(name).getOrElse(default)
    }
  }

  def startLogging( logFilenamePrefix: String = null) = {
    if (!loggingInitialized) {
      loggingInitialized = true
      Config.initializeForTest()
      val logfilenameprefix = Option(logFilenamePrefix).getOrElse( getProp(logFilePrefix, logFilePrefixDefault ) )
      Config.configureFromResource(Config.getPackageNameAsResource(getClass)+"logging.properties", getClass.getClassLoader)
      val handler = new FileHandler(s"${logfilenameprefix}.%d.%u.log")
      handler.setFormatter( new FileFormatter )
      handler.setLevel(Level.ALL)
      Logger.getLogger("").addHandler(handler)
      RedirectOutput.traceStandardOutAndErr()
      testlog.fine(ClassPath.show("    ",getClass.getClassLoader))
    }
  }
}

/**
 * Test class to start the logging system
 */
class TestStartLogging extends FlatSpec with MustMatchers {
  import TestStartLogging._

  behavior of "the start logging test"

  it should "start logging" in {
    startLogging()
  }
}
