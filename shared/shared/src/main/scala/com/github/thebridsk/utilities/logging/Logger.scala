package com.github.thebridsk.utilities.logging

import scala.reflect.macros.blackbox.Context
import scala.reflect.ClassTag
import org.scalactic.source.Position
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory
import scala.annotation.tailrec

trait Logging {
  val resource: String = null
  lazy val logger: Logger = Logger.create(getClass().getName, resource)
}

trait LoggerFactory {
  def isReplaceable = false
  def getLogger(name: String, resource: String = null): Logger
}

object LoggerFactory {
  @volatile var factory: LoggerFactory = null
}

object Logger {

  def apply[T]()(implicit tag: ClassTag[T]): Logger =
    create(tag.runtimeClass.getName, null)
  def apply[T](resource: String)(implicit tag: ClassTag[T]): Logger = {
    if (tag.runtimeClass == classOf[Nothing]) create(resource, null)
    else create(tag.runtimeClass.getName, resource)
  }
  def apply(name: String, resource: String): Logger = create(name, resource)
  def create(name: String, resource: String = null): Logger = {
    import LoggerFactory._
    if (factory == null) LoggerImplFactory.init()
    factory.getLogger(name, resource)
  }

}

abstract class Logger(val name: String, val parent: Option[Logger]) {

  import scala.language.experimental.macros

  private val propogate: Boolean = true
  final private var level: Option[Level] = None

  final def getLevel = level
  final def setLevel(l: Level): Unit = level = Some(l)
  final def resetLevel(): Unit = level = None

  final private def effectiveLevel: Level = // Level.INFO
    {
      getEffectiveLevel
    }

  /**
    * Called by framework to set the effective level
    */
  final def setEffectiveLevel: Unit = {
//    effectiveLevel = getEffectiveLevel
  }

  final def getEffectiveLevel: Level = {
    level match {
      case Some(l) => l
      case None =>
        parent match {
          case Some(p) => p.getEffectiveLevel
          case None =>
            Level.INFO
        }
    }
  }

  private var handlers = List[Handler]()

  def addHandler(handler: Handler): Unit = handlers = handler :: handlers
  def removeHandler(handler: Handler): Unit = handlers = handlers.filter(_ != handler)
  @volatile
  def getHandlers = handlers

  @tailrec
  final protected def logToHandlersAndParent(traceMsg: TraceMsg): Unit = {
    handlers
      .filter { _.isLoggingLevel(traceMsg.level) }
      .foreach(_.log(traceMsg))
    if (propogate) parent match {
      case Some(p) => p.logToHandlersAndParent(traceMsg)
      case None    =>
    }
  }

  @inline
  final def isLoggable(level: com.github.thebridsk.utilities.logging.Level): Boolean =
    level.isLoggable(effectiveLevel)

  def log(level: com.github.thebridsk.utilities.logging.Level, message: String): Unit =
    macro LoggerMacro.logMessage
  def log(
      level: com.github.thebridsk.utilities.logging.Level,
      message: String,
      cause: Throwable
  ): Unit = macro LoggerMacro.logMessageCause
  def log(
      level: com.github.thebridsk.utilities.logging.Level,
      message: String,
      args: Any*
  ): Unit = macro LoggerMacro.logMessageArgs

  def isSevereLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.SEVERE)
  def severe(message: String): Unit = macro LoggerMacro.severeMessage
  def severe(message: String, cause: Throwable): Unit =
    macro LoggerMacro.severeMessageCause
  def severe(message: String, args: Any*): Unit =
    macro LoggerMacro.severeMessageArgs

  def isWarningLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.WARNING)
  def warning(message: String): Unit = macro LoggerMacro.warningMessage
  def warning(message: String, cause: Throwable): Unit =
    macro LoggerMacro.warningMessageCause
  def warning(message: String, args: Any*): Unit =
    macro LoggerMacro.warningMessageArgs

  def isInfoLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.INFO)
  def info(message: String): Unit = macro LoggerMacro.infoMessage
  def info(message: String, cause: Throwable): Unit =
    macro LoggerMacro.infoMessageCause
  def info(message: String, args: Any*): Unit =
    macro LoggerMacro.infoMessageArgs

  def isConfigLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.CONFIG)
  def config(message: String): Unit = macro LoggerMacro.configMessage
  def config(message: String, cause: Throwable): Unit =
    macro LoggerMacro.configMessageCause
  def config(message: String, args: Any*): Unit =
    macro LoggerMacro.configMessageArgs

  def isFineLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.FINE)
  def fine(message: String): Unit = macro LoggerMacro.fineMessage
  def fine(message: String, cause: Throwable): Unit =
    macro LoggerMacro.fineMessageCause
  def fine(message: String, args: Any*): Unit =
    macro LoggerMacro.fineMessageArgs

  def isFinerLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)
  def finer(message: String): Unit = macro LoggerMacro.finerMessage
  def finer(message: String, cause: Throwable): Unit =
    macro LoggerMacro.finerMessageCause
  def finer(message: String, args: Any*): Unit =
    macro LoggerMacro.finerMessageArgs

  def isFinestLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.FINEST)
  def finest(message: String): Unit = macro LoggerMacro.finestMessage
  def finest(message: String, cause: Throwable): Unit =
    macro LoggerMacro.finestMessageCause
  def finest(message: String, args: Any*): Unit =
    macro LoggerMacro.finestMessageArgs

  def isEnteringLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)
  def entering(): Unit = macro LoggerMacro.enteringNoArgs
  def entering(args: Any*): Unit = macro LoggerMacro.enteringArgs

  def isExitingLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)
  def exiting(): Unit = macro LoggerMacro.exitingNoArg
  def exiting(arg: Any): Unit = macro LoggerMacro.exitingArg

  def isThrowingLoggable(): Boolean = isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)
  def throwing(ex: Throwable): Unit = macro LoggerMacro.throwingArg

  def logImpl(traceMsg: TraceMsg): Unit

  def getTime: Double
}

sealed trait TraceType {
  override def toString() = getClass.getSimpleName
}
object LogMsgType extends TraceType
object LogEnterType extends TraceType
object LogExitType extends TraceType
object LogThrowingType extends TraceType

case class TraceMsg(
    val time: Double,
    val logger: String,
    val msgtype: TraceType,
    val level: com.github.thebridsk.utilities.logging.Level,
    val message: String = null,
    val cause: Throwable = null
)(val args: Any*)(implicit val pos: Position) {
  override def toString(): String = {
    s"""TraceMsg($msgtype,$level,"$message",$cause,${pos.fileName}:${pos.lineNumber}""" + args
      .mkString(",", ",", ")")
  }
}

import com.github.thebridsk.utilities.macros.Source

class LoggerMacro(override val c: Context) extends Source(c) {

  def logMessage(
      level: c.Expr[com.github.thebridsk.utilities.logging.Level],
      message: c.Expr[String]
  ): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable($level)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogMsgType, $level, $message)())
        }}"""
  }

  def logMessageCause(
      level: c.Expr[com.github.thebridsk.utilities.logging.Level],
      message: c.Expr[String],
      cause: c.Expr[Throwable]
  ): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable($level)) {
            ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogMsgType, $level, $message, $cause)())
        }}"""
  }

  def logMessageArgs(
      level: c.Expr[com.github.thebridsk.utilities.logging.Level],
      message: c.Expr[String],
      args: c.Expr[Any]*
  ): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable($level)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogMsgType, $level, $message)( ..$args) )
        }}"""
  }

  def severeMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.SEVERE"), message)
  }

  def severeMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.SEVERE"),
      message,
      cause
    )
  }

  def severeMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.SEVERE"),
      message,
      args: _*
    )
  }

  def warningMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.WARNING"), message)
  }

  def warningMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.WARNING"),
      message,
      cause
    )
  }

  def warningMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.WARNING"),
      message,
      args: _*
    )
  }

  def infoMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.INFO"), message)
  }

  def infoMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(c.Expr(q"com.github.thebridsk.utilities.logging.Level.INFO"), message, cause)
  }

  def infoMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.INFO"),
      message,
      args: _*
    )
  }

  def configMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.CONFIG"), message)
  }

  def configMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.CONFIG"),
      message,
      cause
    )
  }

  def configMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.CONFIG"),
      message,
      args: _*
    )
  }

  def fineMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINE"), message)
  }

  def fineMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINE"), message, cause)
  }

  def fineMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINE"),
      message,
      args: _*
    )
  }

  def finerMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINER"), message)
  }

  def finerMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINER"), message, cause)
  }

  def finerMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINER"),
      message,
      args: _*
    )
  }

  def finestMessage(message: c.Expr[String]): c.universe.If = {
    import c.universe._
    logMessage(c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINEST"), message)
  }

  def finestMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    logMessageCause(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINEST"),
      message,
      cause
    )
  }

  def finestMessageArgs(message: c.Expr[String], args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"com.github.thebridsk.utilities.logging.Level.FINEST"),
      message,
      args: _*
    )
  }

  def enteringNoArgs(): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogEnterType, com.github.thebridsk.utilities.logging.Level.FINER)() )
        }}"""
  }

  def enteringArgs(args: c.Expr[Any]*): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogEnterType, com.github.thebridsk.utilities.logging.Level.FINER)(..$args) )
        }}"""
  }

  def exitingNoArg(): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogExitType, com.github.thebridsk.utilities.logging.Level.FINER)() )
        }}"""
  }

  def exitingArg(arg: c.Expr[Any]): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogExitType, com.github.thebridsk.utilities.logging.Level.FINER)($arg) )
        }}"""
  }

  def throwingArg(ex: c.Expr[Throwable]): c.universe.If = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(com.github.thebridsk.utilities.logging.Level.FINER)) {
           ${c.prefix}.logImpl( com.github.thebridsk.utilities.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, com.github.thebridsk.utilities.logging.LogThrowingType, com.github.thebridsk.utilities.logging.Level.FINER, null, $ex)() )
        }}"""
  }

}
