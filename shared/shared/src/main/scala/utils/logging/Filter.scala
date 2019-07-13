package com.github.thebridsk.utilities.logging

trait Filter {
  def isLogged(traceMsg: TraceMsg): Boolean
}

object NoFilter extends Filter {
  def isLogged(traceMsg: TraceMsg) = true
}
