package com.github.thebridsk.utilities.logging

abstract class Handler(
    var level: Level = Level.ALL,
    var formatter: Formatter = DefaultFormatter,
    var filter: Filter = NoFilter
) {

  def isLoggingLevel(l: Level): Boolean = l.isLoggable(level)

  def isLogged(traceMsg: TraceMsg): Boolean = filter.isLogged(traceMsg)

  def logIt(traceMsg: TraceMsg): Unit

  final def log(traceMsg: TraceMsg): Unit = {
    if (isLogged(traceMsg)) logIt(traceMsg)
  }
}
