package com.github.thebridsk.utilities.logging

import com.github.thebridsk.utilities.logging.Level._

class PrintHandler extends Handler {

  def logIt(traceMsg: TraceMsg) = {
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

  def error(msg: String) = {
    println(msg)
  }
  def warning(msg: String) = {
    println(msg)
  }
  def info(msg: String) = {
    println(msg)
  }
  def debug(msg: String) = {
    println(msg)
  }

}
