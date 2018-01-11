package utils.test.logging

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import java.util.logging.LogRecord
import java.util.logging.Level
import utils.logging.MyFormatter
import java.util.TimeZone
import utils.logging.SimpleConsoleFormatter
import java.io.StringReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.logging.LogManager
import utils.test.logging.CaptureStdOutAndErr.RunWithCapture
import utils.logging.ConsoleHandler
import utils.classpath.ClassPath

object TestLogging {

  implicit class CrLfRemover( val s: String ) extends AnyVal {
    def removeTrailingCRLF(): String = {
      if (s == null || s.length() == 0) return s
      val l = s.length()
      var c = l-1
      while ( s.charAt(c) == '\r' || s.charAt(c) == '\n' ) c-=1
      s.substring(0, c+1)
    }
  }

}

import TestLogging._
import java.text.SimpleDateFormat
import java.util.Date

class TestLogging extends FlatSpec with MustMatchers {

  val lineend = System.getProperty("line.separator")

  val runningInEclipse = sys.props.get("RunningInEclipse").map(s=>true).getOrElse(false)

  behavior of "MyFormatter in utilities-macros"

  val fulldate = new SimpleDateFormat("MM-dd HH:mm:ss").format( new Date(0))
  val justtime = new SimpleDateFormat("HH:mm:ss").format( new Date(0))

  it should "format a log record" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter("MM-dd HH:mm:ss",10,10,true,false,false,false,true)

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex ( fulldate+""" \d{10} Test       I Testing""" )
  }

  it should "format an entry log record" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.FINER, "ENTRY {Testing}")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter("MM-dd HH:mm:ss",10,10,true,false,false,false,true)

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex ( fulldate+""" \d{10} Test       > ENTRY \{Testing\}""" )
  }

  it should "format a log record with parameters" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing %s=%d")
    record.setParameters(Array[Object]("arg",1.asInstanceOf[Object]))
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter("HH:mm:ss",10,10,true,false,false,false,true)

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex ( justtime+""" \d{10} Test       I Testing arg=1""" )
  }

  it should "format a log record with shorter thread and logger names" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter("MM-dd HH:mm:ss",5,8,true,false,false,false,true)

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex ( fulldate+""" \d{5} Test     I Testing""" )
  }

  it should "format a log record with simple console formatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new SimpleConsoleFormatter

    val msg = formatter.format(record)
    msg mustBe "Testing"+lineend
  }

  it should "format a log record showing level with simple console formatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.SEVERE, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new SimpleConsoleFormatter( "", useLevelDef=true )

    val msg = formatter.format(record)
    msg mustBe "E Testing"+lineend
  }

  def withLoggerConfiguration( config: String )( fun: Function0[Unit] ): Unit = {
    val outs = new ByteArrayOutputStream
    val outw = new OutputStreamWriter(outs,"UTF8")
    outw.write(config)
    outw.flush()
    val bytes = outs.toByteArray()
    val is = new ByteArrayInputStream(bytes)
    val lm = LogManager.getLogManager
    try {
      lm.readConfiguration(is)

      fun()
    } finally {
      lm.readConfiguration()
    }
  }


  it should "format a log record with empty in config" in withLoggerConfiguration(
      """"""
      ) { () => {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter("MM-dd HH:mm:ss",10,10,true,false,false,false,true)

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex ( fulldate+""" \d{10} Test       I Testing""" )
  }}

  it should "format a log record with threadLen=-1 in config" in withLoggerConfiguration(
      """
      utils.logging.MyFormatter.threadLen = -1
       """
      ) { () => {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter("MM-dd HH:mm:ss",10,10,true,false,false,false,true)

    val msg = formatter.format(record)
    msg mustBe fulldate+" TestThread Test       I Testing"+lineend
  }}

  it should "format a log record with threadLen=-1 and EST in config" in withLoggerConfiguration(
      """
      utils.logging.MyFormatter.threadLen = -1
      utils.logging.MyFormatter.timezone = UTC
      utils.logging.MyFormatter.dateFormat = YYYY-MM-dd HH:mm:ss
       """
      ) { () => {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

//    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
    val formatter = new MyFormatter(null,10,10,true,false,false,false,true)

    val msg = formatter.format(record)
    msg mustBe "1970-01-01 00:00:00 TestThread Test       I Testing"+lineend
  }}

  behavior of "ConsoleHandler in utilities-macros"

  def withCaptureOutput( f: RunWithCapture ): Unit = {
    CaptureStdOutAndErr.runWithCapture(f)
  }

  it should "print log record to stdout" in {
    withCaptureOutput { capture => {
      withLoggerConfiguration(
        """
        utils.logging.MyFormatter.threadLen = -1
        utils.logging.MyFormatter.timezone = UTC
        utils.logging.MyFormatter.dateFormat = MM-dd HH:mm:ss
         """
        ) { () => {
          Thread.currentThread().setName("TestThread")
          val record = new LogRecord(Level.INFO, "Testing")
          record.setLoggerName("Test")

      //    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
          val formatter = new MyFormatter(null,10,10,true,false,false,false,true)

          val handler = new ConsoleHandler
          handler.setFormatter(formatter)

          val expected = "01-01 00:00:00 TestThread Test       I Testing"+lineend

          val msg = formatter.format(record)
          msg mustBe expected

          handler.publish(record)

          val got = capture.getStdout()
//          capture.oldStdOut.println( "got: "+got)
          got mustBe expected
        }}
      }}
    }

  it should "print log record to stdout using default formatter" in {
    withCaptureOutput { capture => {
      withLoggerConfiguration(
        """
        utils.logging.MyFormatter.threadLen = -1
        utils.logging.MyFormatter.timezone = UTC
        utils.logging.MyFormatter.dateFormat = MM-dd HH:mm:ss
         """
        ) { () => {
          Thread.currentThread().setName("TestThread")
          val record = new LogRecord(Level.INFO, "Testing")
          record.setLoggerName("Test")

      //    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
          val formatter = new MyFormatter(null,10,10,true,false,false,false,true)

          val handler = new ConsoleHandler

          val expected = "01-01 00:00:00 TestThread Test       I Testing"+lineend

          val msg = formatter.format(record)
          msg mustBe expected

          handler.publish(record)

          val expectedConsole = """M Test
INFO: Testing""".replace("\n",lineend)+lineend

          val got = capture.getStdout()
//          capture.oldStdOut.println( "got: "+got)
          got must endWith (expectedConsole)
        }}
      }}
    }

  it should "find the formatter class" in {
    //
    // This test is here to confirm the way the java.util.logging
    // system loads handlers and formatters.  It uses the system
    // classloader, which is the classloader that is used to find
    // the main class.  It is typically created from the classpath.
    //
    // This causes problems when running under scalatest in
    // eclipse and sbt.  Only the scalatest launcher is in the
    // classpath, and scalatest creates another classloader to
    // load the tests.
    //
    // This means that handler and formetter classes must be loaded
    // by the test, and NOT using the logging.properties or
    // LogManager.readConfiguration.  The logmanager configuration
    // can be used to configure the handler or formatter.
    //

    val testloader = getClass.getClassLoader
    println(ClassPath.show("Test ClassLoader ", testloader))

    val loader = ClassLoader.getSystemClassLoader
    println(ClassPath.show("SystemClassLoader ", loader))
    try {
      val clz = loader.loadClass("utils.logging.MyFormatter")
      val f = clz.newInstance()
      f.getClass().getName mustBe "utils.logging.MyFormatter"
      if (runningInEclipse) fail("Should not find MyFormatter, if running in sbt,  unset System Property RunningInEclipse")
    } catch {
      case _: ClassNotFoundException =>
        // this is what we are expecting under eclipse and sbt
        if (!runningInEclipse) fail("Failed to find MyFormatter, if running in eclipse, set System Property RunningInEclipse to anything")
      case x: Exception =>
        println("Exception loading myformatter: "+x)
        x.printStackTrace(System.out)
        throw x
    }

  }

  it should "print log record to stdout with MyFormatter" in {
    withCaptureOutput { capture => {
      withLoggerConfiguration(
        """
        utils.logging.ConsoleHandler.level=ALL
        utils.logging.ConsoleHandler.formatter=utils.logging.MyFormatter
        utils.logging.MyFormatter.fakeDate = true
        utils.logging.MyFormatter.threadLen = -1
        utils.logging.MyFormatter.loggerNameLen = 0
        utils.logging.MyFormatter.useMethodName = false
        utils.logging.MyFormatter.timezone = UTC
        utils.logging.MyFormatter.dateFormat = MM-dd HH:mm:ss
         """
        ) { () => {

          Thread.currentThread().setName("ThreadName")
          val record = new LogRecord(Level.INFO, "TestingMessage")
          record.setLoggerName("LoggerName")

      //    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
          val formatter = new MyFormatter(null,10,10,true,false,false,false,true)

          val handler = new ConsoleHandler
          handler.setFormatter(formatter)

          handler.publish(record)

          val expected = "01-01 00:00:00 ThreadName I TestingMessage"+lineend

          val got = capture.getStdout()
//          capture.oldStdOut.println( "got: "+got)
          got mustBe expected
        }}
      }}
    }

}
