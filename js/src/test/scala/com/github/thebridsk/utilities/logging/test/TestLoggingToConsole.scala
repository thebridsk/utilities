package com.github.thebridsk.utilities.logging.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Level
import scala.util.matching.Regex
import com.github.thebridsk.utilities.logging.PrintHandler

class TestLoggingToConsole extends AnyFlatSpec with Matchers {
  behavior of this.getClass.getName+" in utilities-js"

  SystemTimeJs()
  LoggerImplFactory.init()

  implicit val rootHandler: TestHandler = new TestHandler

  val consoleHandler = new PrintHandler

  it should "have a root logger with a level of INFO" in {
    val handlers = Logger("").getHandlers
    handlers.size mustBe 0
    LoggerImplFactory.init(rootHandler,consoleHandler)
    Logger("").getHandlers.size mustBe 2
    val rl = Logger("")
    rl.getLevel match {
      case Some(l) => l mustBe Level.INFO
      case None =>
        fail("Root logger did not have a level defined")
    }
  }

  it should "have a root handler with a level of ALL" in {
    val handlers = Logger("").getHandlers
    handlers.size mustBe 2
    handlers.find( l => l.isInstanceOf[PrintHandler]) match {
      case Some(h: PrintHandler) if h==consoleHandler =>
        h.level mustBe Level.ALL
      case Some(h) =>
        fail("Unknown handler returned: "+h.getClass.getName )
      case None =>
        fail("Did not find console handler")
    }
    handlers.find( l => l.isInstanceOf[TestHandler]) match {
      case Some(h: TestHandler) if h==rootHandler =>
        h.level mustBe Level.ALL
      case Some(h) =>
        if (h == rootHandler) {
            h.level mustBe Level.ALL
        } else {
          fail("Unknown handler returned: "+h.getClass.getName )
        }
      case None =>
        fail("Did not find root handler")
    }
  }

  val testLogger: Logger = Logger[TestLoggingToConsole]()

  it should "have a test logger with a level of None" in {
    testLogger.getLevel match {
      case Some(l) =>
        fail("Test logger must not have a level defined")
      case None =>
    }
  }

  it should "have a test logger with an effective level of INFO" in {
    testLogger.getEffectiveLevel mustBe Level.INFO
  }


  def test( result: Option[String], loggingFun: Function0[Unit] )(implicit handler: TestHandler): Any = {
    handler.clear()
    loggingFun()
    val res = handler.getLog
    result match {
      case Some(r) =>
        val pattern = new Regex("""\d\d?:\d\d:\d\d.\d\d\d """+r)
        res match {
          case pattern() =>
            // found result
          case _ =>
            fail(s"""log did not match ${pattern.toString()}: ${res}""")
        }
      case None =>
        res mustBe ""
    }
  }

  class ExceptionForTest extends Exception( "ExceptionForTest" )

  it should "log at the SEVERE level" in {
    testLogger.isSevereLoggable() mustBe true

    test( Some( """E TestLoggingToConsole\.scala\:97 Hello\n"""), ()=>testLogger.severe("Hello") )
    test( Some( """E TestLoggingToConsole\.scala\:\d+ Hello world\n"""), ()=>testLogger.severe("Hello %s", "world") )
    test( Some( """E TestLoggingToConsole\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.severe("Hello %s for %d", "world",2) )
    test( Some( """E TestLoggingToConsole\.scala\:\d+ Got exception: com\.github\.thebridsk\.utilities\.logging\.test\.TestLoggingToConsole\$ExceptionForTest\: ExceptionForTest\s+at[\s\S.]*"""), ()=>testLogger.severe("Got exception: ", new ExceptionForTest) )
  }

  it should "log at the WARNING level" in {
    testLogger.isWarningLoggable() mustBe true

    test( Some( """W TestLoggingToConsole\.scala\:\d+ Hello\n"""), ()=>testLogger.warning("Hello") )
    test( Some( """W TestLoggingToConsole\.scala\:\d+ Hello world\n"""), ()=>testLogger.warning("Hello %s", "world") )
    test( Some( """W TestLoggingToConsole\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.warning("Hello %s for %d", "world",2) )
    test( Some( """W TestLoggingToConsole\.scala\:\d+ Got exception: com\.github\.thebridsk\.utilities\.logging\.test\.TestLoggingToConsole\$ExceptionForTest\: ExceptionForTest\s+at[\s\S.]*"""), ()=>testLogger.warning("Got exception: ", new ExceptionForTest) )

  }

  it should "log at the INFO level" in {
    testLogger.isInfoLoggable() mustBe true

    test( Some( """I TestLoggingToConsole\.scala\:\d+ Hello\n"""), ()=>testLogger.info("Hello") )
    test( Some( """I TestLoggingToConsole\.scala\:\d+ Hello world\n"""), ()=>testLogger.info("Hello %s", "world") )
    test( Some( """I TestLoggingToConsole\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.info("Hello %s for %d", "world",2) )
    test( Some( """I TestLoggingToConsole\.scala\:\d+ Got exception: com\.github\.thebridsk\.utilities\.logging\.test\.TestLoggingToConsole\$ExceptionForTest\: ExceptionForTest\s+at[\s\S.]*"""), ()=>testLogger.info("Got exception: ", new ExceptionForTest) )

  }

  it should "log at the CONFIG and FINE level" in {
    testLogger.setLevel(Level.FINE)

    testLogger.isFineLoggable() mustBe true

    test( Some( """1 TestLoggingToConsole\.scala\:\d+ Hello\n"""), ()=>testLogger.fine("Hello") )
    test( Some( """1 TestLoggingToConsole\.scala\:\d+ Hello world\n"""), ()=>testLogger.fine("Hello %s", "world") )
    test( Some( """1 TestLoggingToConsole\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.fine("Hello %s for %d", "world",2) )
    test( Some( """1 TestLoggingToConsole\.scala\:\d+ Got exception: com\.github\.thebridsk\.utilities\.logging\.test\.TestLoggingToConsole\$ExceptionForTest\: ExceptionForTest\s+at[\s\S.]*"""), ()=>testLogger.fine("Got exception: ", new ExceptionForTest) )
  }

  it should "clean up" in {
    Logger("").removeHandler(rootHandler)
    Logger("").removeHandler(consoleHandler)
    Logger("").getHandlers.size mustBe 0
  }
}
