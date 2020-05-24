package com.github.thebridsk.utilities.logging

import java.util.{ logging => jul }
import java.{ text => jt, util => ju, lang => jl, io => ji }
import com.github.thebridsk.utilities.classpath.ClassPath
import com.github.thebridsk.utilities.message.Message

object MsgFormatterDefaults {

  val propDateFormat = "dateFormat"
  val propTimezone = "timezone"
  val propFormat = "format"
  val propFakeDate = "fakeDate"
  val propFmtMsg = "fmtMsg"
  val propUseResource = "useResource"
  val propShowKey = "showKey"
  val propAddHeader = "addHeader"
  val propUseThreadName = "useThreadName"

  val defaultDateFormat: String = "yyyy-MM-dd HH:mm:ss:SSS zzz"
  val defaultTimezone: String = null
  val defaultFormat: String = "%1$s %2$s %3$s %4$s %5$s.%6$s %7$s"
  val defaultFakeDate: Boolean = false
  val defaultFmtMsg: Boolean = true
  val defaultUseResource: Boolean = true
  val defaultShowKey: Boolean = false
  val defaultAddHeader: Boolean = true
  val defaultUseThreadName: Boolean = true

  /**
   * If true use java.text.MessageFormat to format message
   * otherwise use com.github.thebridsk.utilities.message.Message
   */
  val useMessageFormat: Boolean = false
}

import MsgFormatterDefaults._

object MsgFormatter {

  private val manager = jul.LogManager.getLogManager()

  private val className = classOf[MsgFormatter].getName()

  def getLoggingProperty( name: String ) = {
    Option(
      manager.getProperty( name )
    )
  }

  /**
    * @return the trace specification
    */
  def getTraceSpec(): String = {
    try {
      val sepChar = System.getProperty("line.separator")
      "Current trace specification: " + sepChar + "  " + new Config()
        .getTraceSpec(sepChar + "  ")
    } catch {
      case x: Exception => // ignore it
        System.out.println("MsgFormatter.getTraceSpec exception: " + x)
        x.printStackTrace(System.out)
        null
    }
  }

  /**
    * @param v additional lines to add to the header
    * @return the header
    */
  def getHeaderString(v: String): String = {
    try {
      val sepChar = System.getProperty("line.separator")

      val builder = new StringBuilder()
      builder
        .append("************ Start Display Current Environment ************")
        .append(sepChar)
      val name = Config.getProgramName()
      if (name != null) {
        builder.append("Program ").append(name).append(sepChar)
      }
      var version = Config.getProgramVersion().getOrElse(null)
      if (version != null) {
        builder.append("Version ").append(version).append(sepChar)
      }
      builder
        .append("Java VM name = ")
        .append(System.getProperty("java.vm.name"))
        .append(sepChar)
      version = System.getProperty("java.fullversion")
      if (version == null) version = System.getProperty("java.runtime.version")
      builder.append("Java Version = ").append(version).append(sepChar)
      // builder.append("Java Compiler = ").append(System.getProperty("java.compiler")   ).append(sepChar)
      Config.getprogramClassLoader() match {
        case Some(l) =>
          builder
            .append("Classpath = ")
            .append(sepChar) // .append(System.getProperty("java.class.path") ).append(sepChar)
          builder.append(ClassPath.show("  ", l))
        case None =>
          builder.append("ClassLoader was not specified").append(sepChar)
      }
      builder
      .append("System Properties")
      .append(sepChar) // .append(System.getProperty("java.class.path") ).append(sepChar)

      builder.append(ClassPath.showProperties("  "))

      if (v != null && v.length() != 0) {
        builder.append(v).append(sepChar)
      }

      builder
        .append("************ End Display Current Environment ************")
        .append(sepChar)
      return builder.toString()
    } catch {
      case x: Exception =>
        System.out.println("MsgFormatter.getHead exception: " + x)
        x.printStackTrace(System.out)
        ""
    }

  }

  var fsParsedForStdout = false

  var fsStdout: jul.Level = null

  var fsStderr: jul.Level = null

  sealed trait MsgType
  object Normal extends MsgType
  object Entry extends MsgType
  object Exit extends MsgType

  /**
    * @param level
    * @param logString the one character string to show in the log for this level
    * @param type
    */
  case class MessageType(
      val level: jul.Level,
      logString: String,
      msgtype: MsgType = Normal
  ) {
    def isNormal = msgtype == Normal
    def isEntry = msgtype == Entry
    def isExit = msgtype == Exit
  }

  private val fsLevels = scala.collection.mutable.Map[jul.Level, MessageType]()

  private def add(level: jul.Level, logString: String) = {
    val mt = MessageType(level, logString)
    fsLevels.put(mt.level, mt)
  }

  add(jul.Level.SEVERE, "E")
  add(jul.Level.WARNING, "W")
  add(jul.Level.INFO, "I")
  add(jul.Level.CONFIG, "C")
  add(jul.Level.FINE, "1")
  add(jul.Level.FINER, "2")
  add(jul.Level.FINEST, "3")

  private val fsEntry = MessageType(jul.Level.FINER, ">", Entry)
  private val fsExit = MessageType(jul.Level.FINER, "<", Exit)
  private val fsThrowing = MessageType(jul.Level.FINER, "T")
  private val fsFiner = MessageType(jul.Level.FINER, "2")
  val fsUnknown = MessageType(null, "?")

  def mapLevelToType(record: jul.LogRecord): MessageType = {
    val level = record.getLevel()

    if (!fsParsedForStdout) {
      fsParsedForStdout = true
      try {
        fsStdout = jul.Level.parse("STDOUT")
        fsStderr = jul.Level.parse("STDERR")
        add(fsStdout, "O")
        add(fsStderr, "R")
      } catch {
        case _: Exception => // ignore
      }
    }

    if (level == jul.Level.FINER) {
      val s = record.getMessage()
      if (s != null) {
        if (testForEntryExit(s, "Entry") || testForEntryExit(s, "ENTRY")) {
          fsEntry
        } else if (testForEntryExit(s, "Exit") || testForEntryExit(s, "RETURN")) {
          fsExit
        } else if (testForEntryExit(s, "THROW")) {
          fsThrowing
        } else {
          fsFiner
        }
      } else {
        fsFiner
      }
    } else {
      fsLevels.get(level).getOrElse(fsUnknown)
    }
  }

  private def testForEntryExit(msg: String, mtype: String) = {
    if (msg.length() < mtype.length()) false
    if (msg.equals(mtype)) true
    if (msg.startsWith(mtype + " {")) true
    else false
  }

  val fsNewLine = System.getProperty("line.separator");

}

import MsgFormatter._

/**
  * A formatter for logging
  *
  * The logger is configurable via custom properties in a logging.properties.
  * The properties are prefixed with the classname of the logger, this might be a derived class.
  * Example: com.github.thebridsk.utilities.logging.MsgFormatter.dateFormat for the dateFormat property.
  *
  * Properties:
  *   dateFormat=<date format string>
  *               A SimpleDateFormat string for the timestamp.  Default <code>yyyy-MM-dd HH:mm:ss:SSS zzz</code>
  *   timezone=<zone>
  *               The timezone to use.  Defaults to local time.
  *   format=<format string>
  *               The format string for the prefix.  The formatting uses java.util.Formatter.
  *               The arguments to the formatter when formatting the strings are:
  *                 1 - timestamp (String)
  *                 2 - thread (String if useThreadName==true, otherwise Long)
  *                 3 - level (String)
  *                 4 - logger name (String)
  *                 5 - classname (String)
  *                 6 - method name (String)
  *                 7 - message (String)
  *                 8 - short classname (String)
  *                 9 - short loggername (String)
  *               default is: <code>%1$s %2$s %3$s %4$s %5$s.%6$s %7$s</code>
  *   fakeDate=<boolean>
  *               Use a fake date.  This is useful when comparing logfiles.  default is false.
  *   fmtMsg=<boolean>
  *               If true, actually format the message.  Default is true.
  *               If false, just print the message code and the arguments.
  *   useResource=<boolean>
  *               If true, use the resource bundle to get format string for message, the default.
  *               If false, will use the hardcoded value for the format string
  *   showKey=<boolean>
  *               If true, the key for the message in the bundle is added for formatted message.
  *               default is false.
  *   addHeader=<boolean>
  *               If true, a header is added to all log files.
  *   useThreadName=<boolean>
  *               If true, use the thread name, the default
  *               otherwise use the thread id
  *
  * @constructor
  * @param defDateFormat
  * @param defTimezone
  * @param defFormat
  * @param defFakeDate
  * @param defFmtMsg
  * @param defUseResource
  * @param defShowKey
  * @param defAddHeader
  * @param defUseThreadName
  */
class MsgFormatter(
  defDateFormat: String = defaultDateFormat,
  defTimezone: String = defaultTimezone,
  defFormat: String = defaultFormat,
  defFakeDate: Boolean = defaultFakeDate,
  defFmtMsg: Boolean = defaultFmtMsg,
  defUseResource: Boolean = defaultUseResource,
  defShowKey: Boolean = defaultShowKey,
  defAddHeader: Boolean = defaultAddHeader,
  defUseThreadName: Boolean = defaultUseThreadName,
) extends jul.Formatter {

  lazy val dateFormat = getProp( propDateFormat, defDateFormat)
  lazy val timezone = getProp( propTimezone, defTimezone)
  lazy val traceFormat = getProp( propFormat, defFormat)
  lazy val fakeDate = getPropBoolean( propFakeDate, defFakeDate)
  lazy val fmtMsg = getPropBoolean( propFmtMsg, defFmtMsg)
  lazy val useResource = getPropBoolean( propUseResource, defUseResource)
  lazy val showKey = getPropBoolean( propShowKey, defShowKey)
  lazy val addHeader = getPropBoolean( propAddHeader, defAddHeader)
  lazy val useThreadName = getPropBoolean( propUseThreadName, defUseThreadName)

  lazy val dateFmt = {
    val df = new jt.SimpleDateFormat( dateFormat )
    if (timezone != null) {
      df.setTimeZone(ju.TimeZone.getTimeZone(timezone))
    }
    df
  }

  def getShortName( fullname: String ) = {
    val sc = fullname.substring(fullname.lastIndexOf('.') + 1)
    val shortClass =
      if (sc.length() == 0) {
        "?"
      } else if (sc.equals("groovy")) {
        var i1 = fullname.lastIndexOf('/')
        val i2 = fullname.lastIndexOf('\\')

        if (i1 < i2) {
          i1 = i2
        }

        if (i1 >= 0) {
          fullname.substring(i1 + 1)
        } else {
          fullname
        }
      } else {
        sc
      }
    shortClass
  }

  override
  def format(record: jul.LogRecord): String = {
    val fbuf = new ju.Formatter()
    val buf = fbuf.out()

    val level = mapLevelToType(record)
    val className = Option(record.getSourceClassName()).getOrElse("")
    val methodName = Option(record.getSourceMethodName()).getOrElse("")
    val loggerName = Option(record.getLoggerName()).getOrElse("")
    val shortClass = getShortName(className)
    val shortLogger = getShortName(loggerName)

    val thread =
      if (useThreadName) {
        Thread.currentThread().getName();
      } else {
        Thread.currentThread().getId()
      }
    val timestamp =
      dateFmt.format(
        new ju.Date( if (fakeDate) 0 else record.getMillis() )
      )

    val msg = formatMessage(record)

    if (traceFormat.indexOf("%-4$.20s")>=0) {
      val e = new Exception("Found a format string of %-4$.20s")
      e.printStackTrace()
      throw e
    }

    fbuf.format(
      traceFormat,
      timestamp,
      thread,
      level.logString,
      loggerName,
      className,
      methodName,
      msg,
      shortClass,
      shortLogger,
    )

    buf.append(fsNewLine);

    // If there's a throwable, handle it.
    val tr = record.getThrown();
    if (null != tr) {
      val sw = new ji.StringWriter();
      val pw = new ji.PrintWriter(sw);
      tr.printStackTrace(pw);
      pw.flush();
      buf.append(sw.toString());
    }

    buf.toString()
  }

  override
  def formatMessage(record: jul.LogRecord): String = {
    formatMessage(mapLevelToType(record), record)
  }

  /**
    * @param mt
    * @param record
    * @return the formatted string
    */
  def formatMessage(mt: MessageType, record: jul.LogRecord) = synchronized {
    var format = record.getMessage()
    var key = ""
    var bundle: ju.ResourceBundle = null
    if (useResource) {
      bundle = record.getResourceBundle()
    }
    if (bundle != null) {
      try {
        val newFmt = bundle.getString(format)
        if (showKey) {
          key = format + " "
        }
        format = newFmt
      } catch {
        case ex: java.util.MissingResourceException => // ignore errors
      }
    }
    try {
      val parameters = record.getParameters()
      if (parameters == null || parameters.length == 0) {
        key + format
      } else {
        var found = false
        if (fmtMsg) {
          if (useMessageFormat) {
            var lastI = 0
            var len = format.length()
            import scala.util.control.Breaks._
            breakable {
              while (!found) {
                lastI = format.indexOf("{", lastI)
                if (lastI < 0) {
                  break
                }
                lastI += 1
                if (lastI >= len) {
                  break
                }
                if (Character.isDigit(format.charAt(lastI))) {
                  found = true
                  break
                }
              }
            }
          } else {
            found = true
          }
        }
        if (found) {
          if (useMessageFormat) {
            key + java.text.MessageFormat.format(format, parameters)
          } else {
            var f: String = ""
            mt.msgtype match {
              case Normal =>
                f = format
              case Entry =>
                val b = new StringBuilder()
                b.append("ENTRY")
                if (parameters != null)
                  b.append(
                    parameters
                      .map { p =>
                        " %s"
                      }
                      .mkString("")
                  )
                f = b.toString()

              case Exit =>
                val b = new StringBuilder()
                b.append("RETURN")
                if (parameters != null)
                  b.append(
                    parameters
                      .map { p =>
                        " %s"
                      }
                      .mkString("")
                  )
                f = b.toString()

              case _ =>
                f = format

            }
            key + Message.getFormattedMessage(null, f, parameters.toIndexedSeq: _*)
          }
        } else {
          val b = new StringBuilder()
          b.append(format)
          for (p <- parameters) {
            b.append(" ")
            b.append(p.toString())
          }
          key + b.toString()
        }
      }

    } catch {
      case _: Exception => record.getMessage()
    }
  }

  override
  def getHead(handler: jul.Handler): String = {
    if (!addHeader) {
      ""
    } else {
      getHeaderString(getTraceSpec())
    }
  }

  override
  def getTail(handler: jul.Handler): String = {
    ""
  }

  lazy val thisClassName = getClass().getName

  def getProp(suffix: String, default: String): String = {
    getLoggingProperty(s"${thisClassName}.${suffix}").getOrElse {
      getLoggingProperty(s"${className}.${suffix}").getOrElse(default)
    }
  }

  def getPropBoolean(suffix: String, default: Boolean): Boolean = {
    getLoggingProperty(s"${thisClassName}.${suffix}").map(_.toBoolean).getOrElse {
      getLoggingProperty(s"${className}.${suffix}").map(_.toBoolean).getOrElse(default)
    }
  }

}
