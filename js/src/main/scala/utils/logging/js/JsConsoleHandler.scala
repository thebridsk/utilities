package utils.logging.js

import scalajs.js.Dynamic.{global => g}
import utils.logging.Level._
import scala.scalajs.js.Any.fromString
import utils.logging.Handler
import utils.logging.TraceMsg
import utils.logging.Level

class JsConsoleHandler extends Handler {

  def logIt( traceMsg: TraceMsg ) {
    if (isLoggingLevel(traceMsg.level)) {
      val s = formatter.format(traceMsg)
      traceMsg.level match {
        case SEVERE|STDERR => error(s)
        case WARNING => warning(s)
        case INFO|STDOUT => info(s)
        case CONFIG|FINE|FINER|FINEST =>
          debug(s)
        case ALL|OFF =>
        case Level(_,_,_,_) =>      }
    }
  }

  def error( msg: String ) = {
    g.console.error(msg)
  }
  def warning( msg: String ) = {
    g.console.warn(msg)
  }
  def info( msg: String ) = {
    g.console.info(msg)
  }
  def debug( msg: String ) = {
    g.console.info(msg)
  }

}

class JsConsoleHandlerInfo extends JsConsoleHandler {

  override
  def error( msg: String ) = {
    g.console.info(msg)
  }
  override
  def warning( msg: String ) = {
    g.console.info(msg)
  }
  override
  def info( msg: String ) = {
    g.console.info(msg)
  }
  override
  def debug( msg: String ) = {
    g.console.info(msg)
  }

}
