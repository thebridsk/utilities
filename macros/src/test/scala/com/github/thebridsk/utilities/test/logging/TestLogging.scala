package com.github.thebridsk.utilities.test.logging

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import java.util.logging.LogRecord
import java.util.logging.Level
import com.github.thebridsk.utilities.logging.SimpleConsoleFormatter
import java.io.OutputStreamWriter
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.logging.LogManager
import com.github.thebridsk.utilities.test.logging.CaptureStdOutAndErr.RunWithCapture
import com.github.thebridsk.utilities.logging.ConsoleHandler
import com.github.thebridsk.utilities.classpath.ClassPath
import com.github.thebridsk.utilities.logging.MsgFormatter
import com.github.thebridsk.utilities.logging.ConsoleFormatter
import com.github.thebridsk.utilities.logging.FileFormatter
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant

object TestLogging {

  implicit class CrLfRemover(private val s: String) extends AnyVal {
    def removeTrailingCRLF(): String = {
      if (s == null || s.length() == 0) return s
      val l = s.length()
      var c = l - 1
      while (s.charAt(c) == '\r' || s.charAt(c) == '\n') c -= 1
      s.substring(0, c + 1)
    }
  }

}

import TestLogging._

class TestLogging extends AnyFlatSpec with Matchers {

  val lineend: String = System.getProperty("line.separator")

  val runningInEclipse: Boolean =
    sys.props.get("RunningInEclipse").map(s => true).getOrElse(false)

  behavior of "logging formatters in utilities-macros"

  val fulldate: String = DateTimeFormatter
    .ofPattern("MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(0))
  val justtime: String = DateTimeFormatter
    .ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(0))

  it should "format a log record with simple console formatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

    val formatter = new SimpleConsoleFormatter

    val msg = formatter.format(record)
    msg mustBe "Testing" + lineend
  }

  it should "format a log record with console formatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

    val formatter = new ConsoleFormatter(defFakeDate = true)

    val msg = formatter.format(record).removeTrailingCRLF()
    msg mustBe justtime + " I TestThread Testing"
  }

  it should "format a log record showing level with simple console formatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.SEVERE, "Testing")
    record.setLoggerName("Test")

    val formatter = new SimpleConsoleFormatter(defFormat = "%3$s %7$s")

    val msg = formatter.format(record)
    msg mustBe "E Testing" + lineend
  }

  def withLoggerConfiguration(config: String)(fun: Function0[Unit]): Unit = {
    val outs = new ByteArrayOutputStream
    val outw = new OutputStreamWriter(outs, "UTF8")
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

  behavior of "ConsoleHandler in utilities-macros"

  def withCaptureOutput(f: RunWithCapture): Unit = {
    CaptureStdOutAndErr.runWithCapture(f)
  }

  it should "print log record to stdout" in {
    withCaptureOutput { capture =>
      {
        withLoggerConfiguration(
          """
        com.github.thebridsk.utilities.logging.MsgFormatter.format = %1$s %2$-10s %4$-10s %3$s %7$s
        com.github.thebridsk.utilities.logging.MsgFormatter.timezone = UTC
        com.github.thebridsk.utilities.logging.MsgFormatter.dateFormat = MM-dd HH:mm:ss
         """
        ) { () =>
          Thread.currentThread().setName("TestThread")
          val record = new LogRecord(Level.INFO, "Testing")
          record.setLoggerName("Test")

          //    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
          val formatter = new MsgFormatter(
            "HH:mm:ss",
            defFakeDate = true,
            defFmtMsg = true,
            defUseResource = true,
            defShowKey = false,
            defAddHeader = false,
            defUseThreadName = true
          )

          val handler = new ConsoleHandler
          handler.setFormatter(formatter)

          val expected =
            "01-01 00:00:00 TestThread Test       I Testing" + lineend

          val msg = formatter.format(record)
          msg mustBe expected

          handler.publish(record)

          val got = capture.getStdout()
//          capture.oldStdOut.println( "got: "+got)
          got mustBe expected
        }
      }
    }
  }

  it should "print log record to stdout using default formatter" in {
    withCaptureOutput { capture =>
      {
        withLoggerConfiguration(
          """
        com.github.thebridsk.utilities.logging.MsgFormatter.format = %1$s %2$-10s %4$-10s %3$s %7$s
        com.github.thebridsk.utilities.logging.MsgFormatter.timezone = UTC
        com.github.thebridsk.utilities.logging.MsgFormatter.dateFormat = MM-dd HH:mm:ss
         """
        ) { () =>
          {
            Thread.currentThread().setName("TestThread")
            val record = new LogRecord(Level.INFO, "Testing")
            record.setLoggerName("Test")

            //    dateFmt, threadLen, loggerNameLen, useFakeDate, useResource, useMethodName, addHeader, useLevel
            val formatter = new MsgFormatter(
              "HH:mm:ss",
              defFakeDate = true,
              defFmtMsg = true,
              defUseResource = true,
              defShowKey = false,
              defAddHeader = false,
              defUseThreadName = true
            )

            val handler = new ConsoleHandler

            val expected =
              "01-01 00:00:00 TestThread Test       I Testing" + lineend

            val msg = formatter.format(record)
            msg mustBe expected

            handler.publish(record)

            val expectedConsole = """M Test
                                    |INFO: Testing
                                    |""".stripMargin.replace("\n", lineend)

            val got = capture.getStdout()
//          capture.oldStdOut.println( "got: "+got)
            got must endWith(expectedConsole)
          }
        }
      }
    }
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
      val clz =
        loader.loadClass("com.github.thebridsk.utilities.logging.MsgFormatter")
      val f = clz.getDeclaredConstructor().newInstance()
      f.getClass()
        .getName mustBe "com.github.thebridsk.utilities.logging.MsgFormatter"
      if (runningInEclipse)
        fail(
          "Should not find MsgFormatter, if running in sbt,  unset System Property RunningInEclipse"
        )
    } catch {
      case _: ClassNotFoundException =>
        // this is what we are expecting under eclipse and sbt
        if (!runningInEclipse)
          fail(
            "Failed to find MsgFormatter, if running in eclipse, set System Property RunningInEclipse to anything"
          )
      case x: Exception =>
        println("Exception loading MsgFormatter: " + x)
        x.printStackTrace(System.out)
        throw x
    }

  }

  behavior of "MsgFormatter in utilities-macros"

  it should "format a log record with MsgFormatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

    val formatter =
      new MsgFormatter(
        defDateFormat = "MM-dd HH:mm:ss",
        // defTimezone = "UTC",

        // 1 - timestamp (String)
        // 2 - thread (String if useThreadName==true, otherwise Long)
        // 3 - level (String)
        // 4 - logger name (String)
        // 5 - classname (String)
        // 6 - method name (String)
        // 7 - message (String)
        // 8 - short classname (String)
        // 9 - short loggername (String)
        defFormat = "%1$s %2$010d %4$-10s %3$s %7$s",
        defFakeDate = true,
        defFmtMsg = false,
        defUseResource = true,
        defShowKey = false,
        defAddHeader = false,
        defUseThreadName = false
      )

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex (fulldate + """ \d{10} Test       I Testing""")
  }

  it should "format an entry log record with MsgFormatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.FINER, "ENTRY {Testing}")
    record.setLoggerName("Test")

    val formatter =
      new MsgFormatter(
        defDateFormat = "MM-dd HH:mm:ss",
        // defTimezone = "UTC",

        // 1 - timestamp (String)
        // 2 - thread (String if useThreadName==true, otherwise Long)
        // 3 - level (String)
        // 4 - logger name (String)
        // 5 - classname (String)
        // 6 - method name (String)
        // 7 - message (String)
        // 8 - short classname (String)
        // 9 - short loggername (String)
        defFormat = "%1$s %2$010d %4$-10s %3$s %7$s",
        defFakeDate = true,
        defFmtMsg = true,
        defUseResource = true,
        defShowKey = false,
        defAddHeader = false,
        defUseThreadName = false
      )

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex (fulldate + """ \d{10} Test       > ENTRY \{Testing\}""")
  }

  it should "format a log record with parameters with MsgFormatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing %s=%d")
    record.setParameters(Array[Object]("arg", 1.asInstanceOf[Object]))
    record.setLoggerName("Test")

    val formatter =
      new MsgFormatter(
        defDateFormat = "HH:mm:ss",
        // defTimezone = "UTC",

        // 1 - timestamp (String)
        // 2 - thread (String if useThreadName==true, otherwise Long)
        // 3 - level (String)
        // 4 - logger name (String)
        // 5 - classname (String)
        // 6 - method name (String)
        // 7 - message (String)
        // 8 - short classname (String)
        // 9 - short loggername (String)
        defFormat = "%1$s %2$010d %4$-10s %3$s %7$s",
        defFakeDate = true,
        defFmtMsg = true,
        defUseResource = true,
        defShowKey = false,
        defAddHeader = false,
        defUseThreadName = false
      )

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex (justtime + """ \d{10} Test       I Testing arg=1""")
  }

  it should "format a log record with shorter thread and logger names with MsgFormatter" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

    val formatter =
      new MsgFormatter(
        defDateFormat = "MM-dd HH:mm:ss",
        // defTimezone = "UTC",

        // 1 - timestamp (String)
        // 2 - thread (String if useThreadName==true, otherwise Long)
        // 3 - level (String)
        // 4 - logger name (String)
        // 5 - classname (String)
        // 6 - method name (String)
        // 7 - message (String)
        // 8 - short classname (String)
        // 9 - short loggername (String)
        defFormat = "%1$s %2$05d %4$-8s %3$s %7$s",
        defFakeDate = true,
        defFmtMsg = true,
        defUseResource = true,
        defShowKey = false,
        defAddHeader = false,
        defUseThreadName = false
      )

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex (fulldate + """ \d{5} Test     I Testing""")
  }

  it should "format a log record with MsgFormatter using thread name" in {
    Thread.currentThread().setName("TestThread")
    val record = new LogRecord(Level.INFO, "Testing")
    record.setLoggerName("Test")

    val formatter =
      new MsgFormatter(
        defDateFormat = "MM-dd HH:mm:ss",
        // defTimezone = "UTC",

        // 1 - timestamp (String)
        // 2 - thread (String if useThreadName==true, otherwise Long)
        // 3 - level (String)
        // 4 - logger name (String)
        // 5 - classname (String)
        // 6 - method name (String)
        // 7 - message (String)
        // 8 - short classname (String)
        // 9 - short loggername (String)
        defFormat = "%1$s %2$-11s %4$-10s %3$s %7$s",
        defFakeDate = true,
        defFmtMsg = false,
        defUseResource = true,
        defShowKey = false,
        defAddHeader = false,
        defUseThreadName = true
      )

    val msg = formatter.format(record).removeTrailingCRLF()
    msg must fullyMatch regex (fulldate + """ TestThread  Test       I Testing""")
  }

  it should "format a log record with empty in config with MsgFormatter" in withLoggerConfiguration(
    """"""
  ) { () =>
    {
      Thread.currentThread().setName("TestThread")
      val record = new LogRecord(Level.INFO, "Testing")
      record.setLoggerName("Test")

      val formatter =
        new MsgFormatter(
          defDateFormat = "MM-dd HH:mm:ss",
          // defTimezone = "UTC",

          // 1 - timestamp (String)
          // 2 - thread (String if useThreadName==true, otherwise Long)
          // 3 - level (String)
          // 4 - logger name (String)
          // 5 - classname (String)
          // 6 - method name (String)
          // 7 - message (String)
          // 8 - short classname (String)
          // 9 - short loggername (String)
          defFormat = "%1$s %2$010d %4$-10s %3$s %7$s",
          defFakeDate = true,
          defFmtMsg = false,
          defUseResource = true,
          defShowKey = false,
          defAddHeader = false,
          defUseThreadName = false
        )

      val msg = formatter.format(record).removeTrailingCRLF()
      msg must fullyMatch regex (fulldate + """ \d{10} Test       I Testing""")
    }
  }

  it should "format a log record with threadLen=-1 in config with MsgFormatter" in withLoggerConfiguration(
    """
      com.github.thebridsk.utilities.logging.MsgFormatter.format=%1$s %2$s %4$-10s %3$s %7$s
       """
  ) { () =>
    {
      Thread.currentThread().setName("TestThread")
      val record = new LogRecord(Level.INFO, "Testing")
      record.setLoggerName("Test")

      val formatter =
        new MsgFormatter(
          defDateFormat = "MM-dd HH:mm:ss",
          // defTimezone = "UTC",

          // 1 - timestamp (String)
          // 2 - thread (String if useThreadName==true, otherwise Long)
          // 3 - level (String)
          // 4 - logger name (String)
          // 5 - classname (String)
          // 6 - method name (String)
          // 7 - message (String)
          // 8 - short classname (String)
          // 9 - short loggername (String)
          defFormat = "%1$s %2$-12s %4$-10s %3$s %7$s",
          defFakeDate = true,
          defFmtMsg = false,
          defUseResource = true,
          defShowKey = false,
          defAddHeader = false,
          defUseThreadName = true
        )

      val msg = formatter.format(record).removeTrailingCRLF()
      msg mustBe fulldate + " TestThread Test       I Testing"
    }
  }

  it should "format a log record with threadLen=-1 and EST in config with MsgFormatter" in withLoggerConfiguration(
    """
      com.github.thebridsk.utilities.logging.MsgFormatter.format = %1$s %2$-10s %4$-10s %3$s %7$s
      com.github.thebridsk.utilities.logging.MsgFormatter.timezone = UTC
      com.github.thebridsk.utilities.logging.MsgFormatter.dateFormat = YYYY-MM-dd HH:mm:ss
       """
  ) { () =>
    {
      Thread.currentThread().setName("TestThread")
      val record = new LogRecord(Level.INFO, "Testing")
      record.setLoggerName("Test")

      val formatter =
        new MsgFormatter(
          defDateFormat = "MM-dd HH:mm:ss",
          // defTimezone = "UTC",

          // 1 - timestamp (String)
          // 2 - thread (String if useThreadName==true, otherwise Long)
          // 3 - level (String)
          // 4 - logger name (String)
          // 5 - classname (String)
          // 6 - method name (String)
          // 7 - message (String)
          // 8 - short classname (String)
          // 9 - short loggername (String)
          defFormat = "%1$s %2$-10s %4$-10s %3$s %7$s",
          defFakeDate = true,
          defFmtMsg = false,
          defUseResource = true,
          defShowKey = false,
          defAddHeader = false,
          defUseThreadName = true
        )

      val msg = formatter.format(record)
      msg mustBe "1970-01-01 00:00:00 TestThread Test       I Testing" + lineend
    }
  }

  it should "format a log record with format in config with SimpleConsoleFormatter" in withLoggerConfiguration(
    """
      com.github.thebridsk.utilities.logging.SimpleConsoleFormatter.format=%3$s %7$s
      """
  ) { () =>
    {
      Thread.currentThread().setName("TestThread")
      val record = new LogRecord(Level.INFO, "Testing")
      record.setLoggerName("Test")

      val formatter = new SimpleConsoleFormatter

      formatter.thisClassName mustBe "com.github.thebridsk.utilities.logging.SimpleConsoleFormatter"
      formatter.getProp(
        "format",
        "<Did not find format value in props>"
      ) mustBe "%3$s %7$s"
      formatter.traceFormat mustBe "%3$s %7$s"

      val msg = formatter.format(record).removeTrailingCRLF()
      msg mustBe "I Testing"
    }
  }

  it should "show that all the formatters have a constructor with no arguments" in {

    def checkConstructor[T](cls: Class[T]) = {
      withClue(
        s"Check if ${cls.getName} has constructor with no arguments, required for LogManager to instantiate"
      ) {
        try {
          cls.getConstructor()
        } catch {
          case e: NoSuchMethodException =>
            fail("Constructor with no arguments not found")
        }
      }
    }

    checkConstructor(classOf[ConsoleFormatter])
    checkConstructor(classOf[SimpleConsoleFormatter])
    checkConstructor(classOf[FileFormatter])
  }
}
