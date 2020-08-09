package com.github.thebridsk.utilities.logging

import java.util.{Formatter => JFormatter}
import java.io.StringWriter
import java.io.PrintWriter
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory

trait Formatter {
  def format(traceMsg: TraceMsg): String
}

object DefaultFormatter extends Formatter {
  def format(traceMsg: TraceMsg): String = {
    import traceMsg._
    msgtype match {
      case LogMsgType =>
        getPrefix(traceMsg) + " " + getMsg(traceMsg) + Option(cause)
          .map(getException(_))
          .getOrElse("")
      case LogEnterType =>
        getPrefix(traceMsg) + " Enter " + args.mkString(", ")
      case LogExitType =>
        if (args.isEmpty) getPrefix(traceMsg) + " Exit "
        else getPrefix(traceMsg) + " Exit " + args(0)
      case LogThrowingType =>
        getPrefix(traceMsg) + " Throwing exception: " + getException(cause)
    }
  }

  def getPrefix(traceMsg: TraceMsg): String = {
    import traceMsg._
    LoggerImplFactory.formatTime(
      time
    ) + " " + level.short + " " + pos.fileName + ":" + pos.lineNumber
  }

  def getMsg(traceMsg: TraceMsg): String = {
    import traceMsg._
    if (args.length == 0) message
    else {
      val a = args.asInstanceOf[Seq[Object]]
      new JFormatter().format(message, a: _*).out.toString()
    }
  }

  def getException(e: Throwable): String = {
    val out = new StringWriter
    val pw = new PrintWriter(out)
    e.printStackTrace(pw)
    pw.flush()
    out.toString()
  }
}
