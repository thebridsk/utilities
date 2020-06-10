//
// Created Apr 29, 2012
//

package com.github.thebridsk.utilities.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.text.DateFormat
import java.util.Date
import java.util.HashMap
import java.util.ResourceBundle
import java.util.TimeZone
import java.util.logging.Formatter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord

import com.github.thebridsk.utilities.classpath.ClassPath
import com.github.thebridsk.utilities.message.Message
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant

private object MyFormatterDefaults {
  val CLASS_NAME = classOf[MyFormatter].getName();

  val fsUseMessageFormat = false;

  val fsNewLine = System.getProperty("line.separator");

  /** Property suffix for date-format override. */
  val fsPropDateFmt = "dateFormat";

  /** Default value for date-format override. */
  val fsDefDateFmt = "yyyy-MM-dd HH:mm:ss:SSS zzz";

  /** Property suffix for timezone override. */
  val fsPropTimeZone = "timezone";

  /** Property suffix for logger-name-length override. */
  val fsPropLoggerNameLen = "loggerNameLen";

  /** Default value for logger-name-length. */
  val fsDefLoggerNameLen = 13;

  /** Gets used when logger name len is zero, to mean "no logger name". */
  val fsNoLoggerNameFmt = "";

  /** Property suffix for thread-number-length override. */
  val fsPropThreadLen = "threadLen";

  /** Default value for thread-number-length. */
  val fsDefThreadLen = 8;

  /** Gets used when class len is zero, to mean "no thread number". */
  val fsNoThreadNameFmt = "";

  /** Property suffix for use-fake-date flag. */
  val fsPropFakeDate = "fakeDate";

  /** Default value for use-fake-date flag. */
  val fsDefFakeDate = false;

  /** If we are using a fake date, here is one. */
  val fsValueFakeDate = new Date(0);

  /** Property suffix for do-format-or-not flag. */
  val fsPropFmtMsg = "fmtMsg";

  /** Default value for do-format-or-not flag. */
  val fsDefFmtMsg = true;

  /** Property suffix for load-resource-bundle-or-not flag. */
  val fsPropUseResource = "useResource";

  /** Default value for load-resource-bundle-or-not flag. */
  val fsDefUseResource = true;

  /** Property suffix for prefix-formatted-message-with-key flag. */
  val fsPropShowKey = "showKey";

  /** Default value for prefix-formatted-message-with-key flag. */
  val fsDefShowKey = false;

  /** Property suffix for prefix-formatted-message-with-key flag. */
  val fsPropCompact = "compact";

  /** Default value for prefix-formatted-message-with-key flag. */
  val fsDefCompact = false;

  /** property suffix for use resource flag. */
  val fsPropUseMethodName = "useMethodName";

  /** default value for use resource flag */
  val fsDefUseMethodName = true;

  /** property suffix for use resource flag. */
  val fsPropUseLevel = "useLevel";

  /** default value for use resource flag */
  val fsDefUseLevel = true;

  /** Property suffix for prefix-formatted-message-with-key flag. */
  val fsPropAddHeader = "addHeader";

  /** Default value for prefix-formatted-message-with-key flag. */
  val fsDefAddHeader = true;

  var fsParsedForStdout = false;

  var fsStdout: Level = null;

  var fsStderr: Level = null;

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
      val level: Level,
      logString: String,
      msgtype: MsgType = Normal
  ) {

    def isNormal = msgtype == Normal
    def isEntry = msgtype == Entry
    def isExit = msgtype == Exit

  }

  private val fsLevels = scala.collection.mutable.Map[Level, MessageType]()

  private def add(level: Level, logString: String) = {
    val mt = MessageType(level, logString);
    fsLevels.put(mt.level, mt);
  }

  add(Level.SEVERE, "E");
  add(Level.WARNING, "W");
  add(Level.INFO, "I");
  add(Level.CONFIG, "C");
  add(Level.FINE, "1");
  add(Level.FINER, "2");
  add(Level.FINEST, "3");

  private val fsEntry = MessageType(Level.FINER, ">", Entry);
  private val fsExit = MessageType(Level.FINER, "<", Exit);
  private val fsThrowing = MessageType(Level.FINER, "T");
  private val fsFiner = MessageType(Level.FINER, "2");
  val fsUnknown = MessageType(null, "?");

  def mapLevelToType(record: LogRecord): MessageType = {
    val level = record.getLevel();

    if (!fsParsedForStdout) {
      fsParsedForStdout = true;
      try {
        fsStdout = Level.parse("STDOUT");
        fsStderr = Level.parse("STDERR");
        add(fsStdout, "O");
        add(fsStderr, "R");
      } catch {
        case _: Exception => // ignore
      }
    }

    if (level == Level.FINER) {
      val s = record.getMessage();
      if (s != null) {
        if (testForEntryExit(s, "Entry") || testForEntryExit(s, "ENTRY")) {
          fsEntry;
        } else if (testForEntryExit(s, "Exit") || testForEntryExit(s, "RETURN")) {
          fsExit;
        } else if (testForEntryExit(s, "THROW")) {
          fsThrowing;
        } else {
          fsFiner
        }
      } else {
        fsFiner
      }
    } else {
      fsLevels.get(level).getOrElse(fsUnknown);
    }
  }

  private def testForEntryExit(msg: String, mtype: String) = {
    if (msg.length() < mtype.length()) false;
    if (msg.equals(mtype)) true;
    if (msg.startsWith(mtype + " {")) true;
    else false;
  }

  /**
    * @return the trace specification
    */
  def getTraceSpec(): String = {
    try {
      val sepChar = System.getProperty("line.separator");
      "Current trace specification: " + sepChar + "  " + new Config()
        .getTraceSpec(sepChar + "  ");
    } catch {
      case x: Exception => // ignore it
        System.out.println("MyFormatter.getTraceSpec exception: " + x)
        x.printStackTrace(System.out)
        null;
    }
  }

  /**
    * @param v additional lines to add to the header
    * @return the header
    */
  def getHeaderString(v: String): String = {
    try {
      val sepChar = System.getProperty("line.separator");

      val builder = new StringBuilder();
      builder
        .append("************ Start Display Current Environment ************")
        .append(sepChar);
      val name = Config.getProgramName();
      if (name != null) {
        builder.append("Program ").append(name).append(sepChar);
      }
      var version = Config.getProgramVersion().getOrElse(null);
      if (version != null) {
        builder.append("Version ").append(version).append(sepChar);
      }
      builder
        .append("Java VM name = ")
        .append(System.getProperty("java.vm.name"))
        .append(sepChar);
      version = System.getProperty("java.fullversion");
      if (version == null) version = System.getProperty("java.runtime.version");
      builder.append("Java Version = ").append(version).append(sepChar);
      //            builder.append("Java Compiler = ").append(System.getProperty("java.compiler")   ).append(sepChar);
      Config.getprogramClassLoader() match {
        case Some(l) =>
          builder
            .append("Classpath = ")
            .append(sepChar); // .append(System.getProperty("java.class.path") ).append(sepChar);
          builder.append(ClassPath.show("  ", l));
        case None =>
          builder.append("ClassLoader was not specified").append(sepChar)
      }
      builder
      .append("System Properties")
      .append(sepChar); // .append(System.getProperty("java.class.path") ).append(sepChar);

      builder.append(ClassPath.showProperties("  "));

      if (v != null && v.length() != 0) {
        builder.append(v).append(sepChar);
      }

      builder
        .append("************ End Display Current Environment ************")
        .append(sepChar);
      return builder.toString();
    } catch {
      case _: Exception => ""
    }

  }

}

import MyFormatterDefaults._

/**
  * A formatter for logging
  *
  * Is configurable via custom properties in logging.properties, or wherever the
  * data for that comes from. Specifically, you can override with the classname
  * (either the one you create, which might be a derived class, or this base
  * class), followed by:
  * <dl>
  * <dt><code>.dateFormat=&lt;date format string&gt;</code></dt>
  * <dd>
  * Specify the formatter to use for the date/time field in the record output.
  * Needs to be a format string that can be used by an instance of
  * {@link SimpleDateFormat}. Defaults to
  * <code>yyyy-MM-dd HH:mm:ss:SSS zzz</code>. If you want one that really
  * <em>really</em> mirrors the WAS format, in all its imperfect glory, then you
  * should set this to <code>M/dd/yy HH:mm:ss:SSS zzz</code>.</dd>
  * <dt><code>.timezone=UTC</code></dt>
  * <dd>
  * Force the timezone to UTC. Defaults to local time. Any timezone is possible,
  * but is not recommended.</dd>
  * <dt><code>.loggerNameLen=20</code></dt>
  * <dd>
  * Use 20-character logger name field instead of 13, which is the default.</dd>
  * <dt><code>.threadLen=&lt;n&gt;</code></dt>
  * <dd> Use n-character thread numbers instead of 8, which is the default.
  * Set to zero to not get thread numbers.
  * Set to -1 if thread name should be used instead of the thread id. </dd>
  * <dt><code>.fakeDate=true</code></dt>
  * <dd>
  * Use a fake date/time (the beginning of the epoch) instead of the current
  * date/time, which is the default.</dd>
  * <dt><code>.fmtMsg=true</code></dt>
  * <dd>
  * Actually format messages (the default). Set to <code>false</code> to simply
  * print the message code and the arguments.</dd>
  * <dt><code>.useResource=true</code></dt>
  * <dd>
  * If <code>true</code>, load resource bundles. This is probably what you want.
  * If <code>false</code>, it will not do that.</dd>
  * <dt><code>.showKey=false</code></dt>
  * <dd>
  * If <code>true</code>, prefix every message with the message key. If
  * <code>false</code>, don't do that.</dd>
  * <dt><code>.compact=false</code></dt>
  * <dd>
  * Use a compact format for the entry header. If <code>false</code>, don't do
  * that. Default is <code>false</code>.</dd>
  * <dt><code>.useMethodName=true</code></dt>
  * <dd>if <code>true</code>, write method name in trace entry.  Default is <code>true</code>. </dd>
  * <dt><code>.useLevel=true</code></dt>
  * <dd>if <code>true</code>, write level character in trace entry.  Default is <code>true</code>. </dd>
  * <dt><code>.addHeader=false</code></dt>
  * <dd>
  * add a header to the trace file.
  * Default is <code>true</code>.</dd>
  * </dl>
  *
  * For example: <br />
  * <code>
  *   com.github.thebridsk.utilities.logging.MyFormatter.classLen=20
  * </code> <br />
  * (unless some clown has refactored this into a different package, in which
  * case, <i>caveat emptor</i>.)
  *
  * @constructor
  * @param dateFmtDef
  * @param threadLenDef
  * @param loggerNameLenDef
  * @param useFakeDateDef
  * @param useResourceDef
  * @param useMethodNameDef
  * @param addHeaderDef
  * @param useLevelDef
  *
  * @author werewolf
  */
class MyFormatter(
    dateFmtDef: String,
    threadLenDef: Int = fsDefThreadLen,
    loggerNameLenDef: Int = fsDefLoggerNameLen,
    useFakeDateDef: Boolean = fsDefFakeDate,
    useResourceDef: Boolean = fsDefUseResource,
    useMethodNameDef: Boolean = fsDefUseMethodName,
    addHeaderDef: Boolean = fsDefAddHeader,
    useLevelDef: Boolean = fsDefUseLevel
) extends Formatter {

  def this() = this(fsDefDateFmt)

  private val manager = LogManager.getLogManager();

  private val thisClassName = getClass().getName

  private def getProp(suffix: String, default: String): String = {
    val retVal = manager.getProperty(thisClassName + "." + suffix);

    if (null != retVal) retVal
    else {
      // If that didn't work, try the base class (this one).
      val r = manager.getProperty(CLASS_NAME + "." + suffix);
      if (r == null) default
      else r
    }
  }

  private def getPropBoolean(suffix: String, default: Boolean): Boolean = {
    val retVal = manager.getProperty(thisClassName + "." + suffix);

    if (null != retVal) retVal.toBoolean
    else {
      // If that didn't work, try the base class (this one).
      val r = manager.getProperty(CLASS_NAME + "." + suffix);
      if (r == null) default
      else r.toBoolean
    }
  }

  private val dateFmt = {
    val d = DateTimeFormatter.ofPattern(
      getProp(fsPropDateFmt, dateFmtDef)
    )

    // Read the timezone override.

    // Get the timezone override. First try the class that we
    // constructed.
    val timeZoneStr = getProp(fsPropTimeZone, null);
    // If we have it, set it. If we don't have it, let it default to
    // local.
    if (null != timeZoneStr) {
      d.withZone( ZoneId.of(timeZoneStr))
    } else {
      d
    }
  }

  private val (threadFmt, useThreadName): (String, Boolean) =
    // Read the thread-number-length override.
    {
      var charsInThreadFmt = threadLenDef;

      val charsInThreadStr = getProp(fsPropThreadLen, null);
      // If we have it, set it.
      if (null != charsInThreadStr) {
        try {
          charsInThreadFmt = Integer.parseInt(charsInThreadStr);
        } catch {
          case nfe: NumberFormatException => /* NOP */
        }
      }

      if (charsInThreadFmt > 0) {
        ("%0" + charsInThreadFmt + "d", false)
      } else if (charsInThreadFmt < 0) {
        ("%d", true)
      } else {
        (fsNoThreadNameFmt, false)
      }
    }

  private val loggerNameFmt: String =
    // Read the class-name-length override.
    {
      var charsInClassFmt = loggerNameLenDef;

      val charsInClassStr = getProp(fsPropLoggerNameLen, null);
      // If we have it, set it.
      if (null != charsInClassStr) {
        try {
          charsInClassFmt = Integer.parseInt(charsInClassStr);
        } catch {
          case nfe: NumberFormatException => // NOP
        }
      }

      if (charsInClassFmt > 0) {
        "%-" + charsInClassFmt + "." + charsInClassFmt + "s";
      } else {
        fsNoLoggerNameFmt;
      }
    }

  private val useFakeDate: Boolean =
    getPropBoolean(fsPropFakeDate, useFakeDateDef);

  private var fmtMsgOrNot: Boolean = getPropBoolean(fsPropFmtMsg, fsDefFmtMsg)

  private val useResource: Boolean =
    getPropBoolean(fsPropUseResource, useResourceDef)

  private val showKey: Boolean = getPropBoolean(fsPropShowKey, fsDefShowKey)

  private val compact: Boolean = getPropBoolean(fsPropCompact, fsDefCompact)

  private val useMethodName: Boolean =
    getPropBoolean(fsPropUseMethodName, useMethodNameDef)

  private val useLevel: Boolean = getPropBoolean(fsPropUseLevel, useLevelDef)

  private val addHeader: Boolean = getPropBoolean(fsPropAddHeader, addHeaderDef)

  private def getDate(record: LogRecord): Date = {
    if (useFakeDate) fsValueFakeDate else new Date(record.getMillis())
  }

  private def getLinePrefix(record: LogRecord): String = {

    val dateStr = dateFmt.format( Instant.ofEpochMilli( getDate(record).getTime() ));
    val retVal = if (dateStr.length() > 0) dateStr + " " else "";

    val threadStr =
      if (useThreadName) {
        Thread.currentThread().getName();
      } else {
        String.format(
          threadFmt,
          Thread.currentThread().getId().asInstanceOf[Object]
        );
      }

    if (threadStr.length() > 0) {
      retVal + threadStr + " ";
    } else {
      retVal
    }

  }

  /**
    * Provide a mechanism for derived classes to change the internal setting
    * controlling whether or not message formatting is done once the initial
    * configuration has been read in.
    *
    * This should only be done directly after construction to avoid
    * synchronisation excitement.
    *
    * @param fmtOrNot <code>true</code> to do message formatting,
    *            <code>false</code> to not do it.
    */
  def setFmtOrNot(fmtOrNot: Boolean): Unit = synchronized {
    fmtMsgOrNot = fmtOrNot;
  }

  @Override
  def format(record: LogRecord): String = synchronized {
    val outSB = new StringBuilder();
    val mt =
      if (compact) formatPrefixCompact(outSB, record)
      else formatPrefixNotCompact(outSB, record)

    // Either we're formatting the message, or we're just faking it.
    outSB.append(formatMessage(mt, record));

    outSB.append(fsNewLine);

    // If there's a throwable, handle it.
    val tr = record.getThrown();
    if (null != tr) {
      val sw = new StringWriter();
      val pw = new PrintWriter(sw);
      tr.printStackTrace(pw);
      pw.flush();
      outSB.append(sw.toString());
    }

    outSB.toString();
  }

  private def get(v: String, default: String) = if (v == null) default else v

  private def formatPrefixCompact(
      outSB: StringBuilder,
      record: LogRecord
  ): MessageType = {
    val sv = MyFormatterDefaults.mapLevelToType(record);
    val cn = get(record.getSourceClassName(), record.getLoggerName())
    val mn = get(record.getSourceMethodName(), "")
    var sc = cn.substring(cn.lastIndexOf('.') + 1);
    if (sc.length() == 0) {
      sc = "?";
    }
    if (sc.equals("groovy")) {
      var i1 = cn.lastIndexOf('/');
      val i2 = cn.lastIndexOf('\\');

      if (i1 < i2) {
        i1 = i2;
      }

      if (i1 >= 0) {
        sc = cn.substring(i1 + 1);
      } else {
        sc = cn;
      }
    }

    outSB.append(getLinePrefix(record));

    val classNameStr = String.format(loggerNameFmt, sc);
    if (classNameStr.length() > 0) {
      outSB.append(classNameStr).append(" ");
    }

    if (useLevel) {
      outSB.append(sv.logString);
      outSB.append(" ");
    }
    if (useMethodName) {
      outSB.append(cn);
      outSB.append(" ");
      outSB.append(mn);
      outSB.append(" ");
    }
    return sv;
  }

  private def formatPrefixNotCompact(
      outSB: StringBuilder,
      record: LogRecord
  ): MessageType = {
    val sv = mapLevelToType(record);
    var cn = record.getSourceClassName();
    var mn = record.getSourceMethodName();
    var ln = record.getLoggerName();

    if (null == cn) {
      cn = ln;
      if (ln == null) {
        cn = "";
      }
    }
    if (null == mn) {
      mn = "";
    }
    if (ln == null || ln.length() == 0) {
      ln = "?";
    }
    var sl = ln.substring(ln.lastIndexOf('.') + 1);
    if (sl.length() == 0) {
      sl = "?";
    }

    outSB.append(getLinePrefix(record));

    var classNameStr = String.format(loggerNameFmt, sl);
    if (classNameStr.length() > 0) {
      outSB.append(classNameStr).append(" ");
    }

    if (useLevel) {
      outSB.append(sv.logString);
      outSB.append(" ");
    }
    if (useMethodName) {
      outSB.append(cn);
      outSB.append(" ");
      outSB.append(mn);
      outSB.append(" ");
    }
    return sv;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * java.util.logging.Formatter#formatMessage(java.util.logging.LogRecord)
   */
  override def formatMessage(record: LogRecord) = synchronized {
    formatMessage(fsUnknown, record);
  }

  /**
    * @param mt
    * @param record
    * @return the formatted string
    */
  def formatMessage(mt: MessageType, record: LogRecord) = synchronized {
    var format = record.getMessage();
    var key = "";
    var bundle: ResourceBundle = null;
    if (useResource) {
      bundle = record.getResourceBundle();
    }
    if (bundle != null) {
      try {
        val newFmt = bundle.getString(format);
        if (showKey) {
          key = format + " ";
        }
        format = newFmt;
      } catch {
        case ex: java.util.MissingResourceException => // ignore errors
      }
    }
    try {
      val parameters = record.getParameters();
      if (parameters == null || parameters.length == 0) {
        key + format;
      } else {
        var found = false;
        if (fmtMsgOrNot) {
          if (fsUseMessageFormat) {
            var lastI = 0;
            var len = format.length();
            import scala.util.control.Breaks._
            breakable {
              while (!found) {
                lastI = format.indexOf("{", lastI);
                if (lastI < 0) {
                  break;
                }
                lastI += 1;
                if (lastI >= len) {
                  break;
                }
                if (Character.isDigit(format.charAt(lastI))) {
                  found = true;
                  break;
                }
              }
            }
          } else {
            found = true;
          }
        }
        if (found) {
          if (fsUseMessageFormat) {
            key + java.text.MessageFormat.format(format, parameters);
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
            key + Message.getFormattedMessage(null, f, parameters.toIndexedSeq: _*);
          }
        } else {
          val b = new StringBuilder();
          b.append(format);
          for (p <- parameters) {
            b.append(" ");
            b.append(p.toString());
          }
          key + b.toString()
        }
      }

    } catch {
      case _: Exception => record.getMessage()
    }
  }

  override def getHead(h: Handler): String = {
    val head = if (!addHeader) {
      ""
    } else {
      try {
        getHeaderString(getTraceSpec());
      } catch {
        case x: Throwable =>
          System.out.println("MyFormatter.getHead exception: " + x)
          x.printStackTrace(System.out)
          ""
      }
    }
//      System.out.println("MyFormatter.getHead: "+head)
    head

  }

}
