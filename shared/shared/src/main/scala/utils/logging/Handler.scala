package utils.logging

abstract class Handler( var level: Level = Level.ALL,
                        var formatter: Formatter = DefaultFormatter,
                        var filter: Filter = NoFilter) {

  def isLoggingLevel( l: Level ) = l.isLoggable(level)

  def isLogged( traceMsg: TraceMsg ) = filter.isLogged(traceMsg)

  def logIt( traceMsg: TraceMsg ): Unit

  final def log( traceMsg: TraceMsg ) = {
    if (isLogged(traceMsg)) logIt(traceMsg)
  }
}
