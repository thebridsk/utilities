package com.github.thebridsk.utilities.logging

import java.io.FilterOutputStream
import java.io.ByteArrayOutputStream
import java.util.logging.{Logger => JLogger}
import scala.util.matching.Regex
import java.util.regex.Matcher
import java.io.FilterWriter
import java.io.CharArrayWriter
import java.util.logging.Level

object TraceOutputStream {
  val fsLines = "\\r\\n|\\r|\\n".r

  private object StackRetriever extends SecurityManager {

    def getCurrentStack(): Array[Class[_]] = {
      return getClassContext();
    }

  }

  private val fsJavaUtilLogging = "java.util.logging.";

  /**
    * @return true if this callstack has java.util.logging in it.
    */
  def isFromLogging(): Boolean = {
    val clses = StackRetriever.getCurrentStack();
    var len = clses.length;
    if (len > 20) {
      len = 20;
    }
    for (i <- 1 until len) {
      if (clses(i).getName().startsWith(fsJavaUtilLogging)) {
        return true;
      }
    }
    return false;
  }

}

class TraceOutputStream(
    level: Level = Level.INFO,
    loggername: String = classOf[TraceWriter].getName()
) extends FilterOutputStream(new ByteArrayOutputStream) {
  import TraceWriter._
  private val fLog: JLogger =
    JLogger.getLogger(loggername, null /* resource bundle */ )

  override def write(b: Array[Byte]): Unit = synchronized {
    if (!isFromLogging()) {
      out.write(b, 0, b.length);
      b.find { c =>
        c == '\n' || c == '\r'
      } match {
        case Some(_) => flush
        case _       =>
      }
    }
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = synchronized {
    if (!isFromLogging()) {
      out.write(b, off, len);
      var foundcrlf = false;
      val end = off + len;
      import scala.util.control.Breaks._
      breakable {
        for (i <- off until end) {
          val c = b(i);
          if (c == '\n' || c == '\r') {
            foundcrlf = true;
            break()
          }
        }
      }
      if (foundcrlf) {
        flush();
      }
    }
  }

  override def write(b: Int): Unit = synchronized {
    if (!isFromLogging()) {
      out.write(b);
      if (b == '\n' || b == '\r') {
        flush();
      }
    }
  }

  override def close() = out.close()

  private def fOut = out.asInstanceOf[ByteArrayOutputStream]

  def reset() = fOut.reset()

  override def flush(): Unit = {
    if (!isFromLogging()) {
      var message: String = null;
      synchronized {
        out.flush();
        message = out.toString();
        reset();
      }
      if (message.length() > 0) {
        val i = 0;
        val m = fsLines.split(message)
        for (l <- m) {
          processLine(l)
        }
      }
    }
  }

  private def processLine(line: String) = {
    if (fLog.isLoggable(level)) {
      fLog.logp(level, "", "", "%s", line);
    }
  }

}
