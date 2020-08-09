package com.github.thebridsk.utilities.logging.js

import scalajs.js.Dynamic.{global => g}
import com.github.thebridsk.utilities.logging.Level._
import scala.scalajs.js.Any.fromString
import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.TraceMsg
import com.github.thebridsk.utilities.logging.Level

class JsConsoleHandler extends Handler {

  def logIt(traceMsg: TraceMsg): Unit = {
    if (isLoggingLevel(traceMsg.level)) {
      val s = formatter.format(traceMsg)
      traceMsg.level match {
        case SEVERE | STDERR => error(s)
        case WARNING         => warning(s)
        case INFO | STDOUT   => info(s)
        case CONFIG | FINE | FINER | FINEST =>
          debug(s)
        case ALL | OFF         =>
        case Level(_, _, _, _) =>
      }
    }
  }

  def error(msg: String): Unit = {
    g.console.error(msg)
  }
  def warning(msg: String): Unit = {
    g.console.warn(msg)
  }
  def info(msg: String): Unit = {
    g.console.info(msg)
  }
  def debug(msg: String): Unit = {
    g.console.info(msg)
  }

}

class JsConsoleHandlerInfo extends JsConsoleHandler {

  override def error(msg: String): Unit = {
    g.console.info(msg)
  }
  override def warning(msg: String): Unit = {
    g.console.info(msg)
  }
  override def info(msg: String): Unit = {
    g.console.info(msg)
  }
  override def debug(msg: String): Unit = {
    g.console.info(msg)
  }

}
