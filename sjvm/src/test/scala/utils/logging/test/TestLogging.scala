package utils.logging.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import utils.logging.impl.LoggerImplFactory
import utils.logging.Logger
import utils.logging.Handler
import utils.logging.Level
import scala.util.matching.Regex
import org.scalactic.source.Position

class TestLogging extends FlatSpec with MustMatchers {
  behavior of this.getClass.getName+" in utilities-jvm"

  SystemTimeJvm()
  implicit val rootHandler = new TestHandler
  LoggerImplFactory.init()

  it should "have a root logger with a level of INFO" in {
    val handlers = Logger("").getHandlers()
    handlers.size mustBe 0
    LoggerImplFactory.init(rootHandler)
    Logger("").getHandlers().size mustBe 1
    val rl = Logger("")
    rl.getLevel match {
      case Some(l) => l mustBe Level.INFO
      case None =>
        fail("Root logger did not have a level defined")
    }
  }

  it should "have a root handler with a level of ALL" in {
    Logger("").getHandlers().find( l => l.isInstanceOf[TestHandler]) match {
      case Some(h: TestHandler) if h==rootHandler =>
        h.level mustBe Level.ALL
      case Some(h) =>
        fail("Unknown handler returned: "+h.getClass.getName )
      case None =>
        fail("Did not find root handler")
    }
  }

  val testLogger = Logger[TestLogging]

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

  import scala.language.postfixOps

  def test( result: Option[String], loggingFun: Function0[Unit] )(implicit handler: TestHandler, pos: Position) = {
    handler.clear()
    loggingFun()
    val res = handler.getLog()
    result match {
      case Some(r) =>
        val pattern = new Regex("""\d\d:\d\d:\d\d.\d\d\d """+r)
        res match {
          case pattern() =>
            // found result
          case _ =>
            fail(s"""From ${pos.fileName}:${pos.lineNumber} log did not match ${r}: ${res}""")
        }
      case None =>
        res mustBe ""
    }
  }

  it should "log at the SEVERE level" in {
    testLogger.isSevereLoggable() mustBe true

    test( Some( """E TestLogging\.scala\:80 Hello\n"""), ()=>testLogger.severe("Hello") )
    test( Some( """E TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.severe("Hello %s", "world") )
    test( Some( """E TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.severe("Hello %s for %d", "world",2) )
    test( Some( """E TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.severe("Got exception: ", new NullPointerException) )
  }

  it should "log at the WARNING level" in {
    testLogger.isWarningLoggable() mustBe true

    test( Some( """W TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.warning("Hello") )
    test( Some( """W TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.warning("Hello %s", "world") )
    test( Some( """W TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.warning("Hello %s for %d", "world",2) )
    test( Some( """W TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.warning("Got exception: ", new NullPointerException) )

  }

  it should "log at the INFO level" in {
    testLogger.isInfoLoggable() mustBe true

    test( Some( """I TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.info("Hello") )
    test( Some( """I TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.info("Hello %s", "world") )
    test( Some( """I TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.info("Hello %s for %d", "world",2) )
    test( Some( """I TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.info("Got exception: ", new NullPointerException) )

  }

  it should "log at the STDOUT and STDERR levels" in {
    testLogger.isLoggable(Level.STDERR) mustBe true
    testLogger.isLoggable(Level.STDOUT) mustBe true
  }

  it should "not log at the CONFIG, FINE, FINER, FINEST levels" in {
    testLogger.isConfigLoggable() mustBe false
    testLogger.isFineLoggable() mustBe false
    testLogger.isFinerLoggable() mustBe false
    testLogger.isFinestLoggable() mustBe false

    test( None, ()=>testLogger.config("Hello") )
    test( None, ()=>testLogger.fine("Hello") )
    test( None, ()=>testLogger.finer("Hello") )
    test( None, ()=>testLogger.finest("Hello") )
  }

  it should "log at the CONFIG and FINE level" in {
    testLogger.setLevel(Level.FINE)

    testLogger.isSevereLoggable() mustBe true
    testLogger.isWarningLoggable() mustBe true
    testLogger.isInfoLoggable() mustBe true

    test( Some( """E TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.severe("Hello") )

    testLogger.isConfigLoggable() mustBe true
    testLogger.isFineLoggable() mustBe true

    test( Some( """C TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.config("Hello") )
    test( Some( """C TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.config("Hello %s", "world") )
    test( Some( """C TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.config("Hello %s for %d", "world",2) )
    test( Some( """C TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.config("Got exception: ", new NullPointerException) )

    test( Some( """1 TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.fine("Hello") )
    test( Some( """1 TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.fine("Hello %s", "world") )
    test( Some( """1 TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.fine("Hello %s for %d", "world",2) )
    test( Some( """1 TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.fine("Got exception: ", new NullPointerException) )

    testLogger.isFinerLoggable() mustBe false
    testLogger.isFinestLoggable() mustBe false

    test( None, ()=>testLogger.finer("Hello") )
    test( None, ()=>testLogger.finest("Hello") )
  }

  it should "log at the all levels" in {
    testLogger.setLevel(Level.ALL)

    testLogger.isSevereLoggable() mustBe true
    testLogger.isWarningLoggable() mustBe true
    testLogger.isInfoLoggable() mustBe true

    test( Some( """E TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.severe("Hello") )

    testLogger.isConfigLoggable() mustBe true
    testLogger.isFineLoggable() mustBe true

    testLogger.isFinerLoggable() mustBe true
    testLogger.isFinestLoggable() mustBe true

    test( Some( """2 TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.finer("Hello") )
    test( Some( """2 TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.finer("Hello %s", "world") )
    test( Some( """2 TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.finer("Hello %s for %d", "world",2) )
    test( Some( """2 TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.finer("Got exception: ", new NullPointerException) )

    test( Some( """3 TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.finest("Hello") )
    test( Some( """3 TestLogging\.scala\:\d+ Hello world\n"""), ()=>testLogger.finest("Hello %s", "world") )
    test( Some( """3 TestLogging\.scala\:\d+ Hello world for 2\n"""), ()=>testLogger.finest("Hello %s for %d", "world",2) )
    test( Some( """3 TestLogging\.scala\:\d+ Got exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.finest("Got exception: ", new NullPointerException) )

  }

  it should "log fine to root handler, but not extra root handler, info to both" in {
    val extraRootHandler = new TestHandler
    Logger("").addHandler(extraRootHandler)
    try {
      Logger("").getHandlers.size mustBe 2
      extraRootHandler.level = Level.INFO

      testLogger.isFineLoggable() mustBe true
      test( Some( """2 TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.finer("Hello") )
      test( None, ()=>testLogger.fine("Hello") )( extraRootHandler, Position.here )

      testLogger.isFineLoggable() mustBe true
      test( Some( """I TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.info("Hello") )
      test( Some( """I TestLogging\.scala\:\d+ Hello\n"""), ()=>testLogger.info("Hello") )(extraRootHandler, Position.here)

    } finally {
      Logger("").removeHandler(extraRootHandler)
    }
    Logger("").getHandlers.size mustBe 1
  }

  it should "log ENTRY, EXIT, and THROWING" in {
    rootHandler.isLoggingLevel(Level.FINER) mustBe true
    test( Some( """2 TestLogging\.scala\:\d+ Enter \n"""), ()=>testLogger.entering() )
    test( Some( """2 TestLogging\.scala\:\d+ Exit \n"""), ()=>testLogger.exiting() )
    test( Some( """2 TestLogging\.scala\:\d+ Throwing exception: java.lang.NullPointerException\s+at[\s\S.]*"""), ()=>testLogger.throwing( new NullPointerException ) )
  }

  it should "log ENTRY and EXIT with arguments" in {
    test( Some( """2 TestLogging\.scala\:\d+ Enter hello\n"""), ()=>testLogger.entering("hello") )
    test( Some( """2 TestLogging\.scala\:\d+ Exit goodbye\n"""), ()=>testLogger.exiting("goodbye") )
    test( Some( """2 TestLogging\.scala\:\d+ Enter hello, world\n"""), ()=>testLogger.entering("hello","world") )
    test( Some( """2 TestLogging\.scala\:\d+ Exit goodbye\n"""), ()=>testLogger.exiting("goodbye") )
  }

  val test2logger = Logger("comm.Sending")

  val utilsHandler = new TestHandler

  it should "add utilsHandler to utils logger" in {
    Logger("utils").addHandler(utilsHandler)
    Logger("utils").getHandlers().size mustBe 1
  }

  it should "log to both utilsHandler and rootHandler a message from testLogger" in {
    rootHandler.clear()
    utilsHandler.clear()
    testLogger.info("Going to both")
    val loggedmsg = """\d\d:\d\d:\d\d.\d\d\d I TestLogging\.scala\:\d+ Going to both\n"""
    rootHandler.getLog() must fullyMatch regex loggedmsg
    utilsHandler.getLog() must fullyMatch regex loggedmsg
  }

  it should "log only to rootHandler a message from test2Logger" in {
    rootHandler.clear()
    utilsHandler.clear()
    test2logger.info("Going to rootHandler")
    val loggedmsg = """\d\d:\d\d:\d\d.\d\d\d I TestLogging\.scala\:\d+ Going to rootHandler\n"""
    rootHandler.getLog must fullyMatch regex loggedmsg
    utilsHandler.getLog() mustBe ""
  }

  it should "clean up" in {
    Logger("").removeHandler(rootHandler)
    Logger("").getHandlers().size mustBe 0
    Logger("utils").removeHandler(utilsHandler)
    Logger("utils").getHandlers().size mustBe 0
  }
}
