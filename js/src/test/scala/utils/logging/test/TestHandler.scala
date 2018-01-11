package utils.logging.test

import utils.logging.Handler
import utils.logging.TraceMsg
import utils.logging.Level
import utils.logging.Level._

class TestHandler extends Handler {

  val buf = StringBuilder.newBuilder

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
        case Level(_,_,_,_) =>
      }
    }
  }

  def error( msg: String ) = {
    buf ++= msg ++= "\n"
  }
  def warning( msg: String ) = {
    buf ++= msg ++= "\n"
  }
  def info( msg: String ) = {
    buf ++= msg ++= "\n"
  }
  def debug( msg: String ) = {
    buf ++= msg ++= "\n"
  }

  def getLog() = buf.toString()

  def clear() = buf.clear()
}
