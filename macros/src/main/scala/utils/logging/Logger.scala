package com.github.thebridsk.utilities.logging

import language.experimental.macros
import java.util.logging.{Logger => JavaLogger}
import scala.reflect.macros.blackbox.Context
import scala.reflect.ClassTag

trait Logging {
  val resource: String = null
  lazy val logger = Logger(getClass().getName, resource)
}

object Logger {
  def apply[T]()(implicit tag: ClassTag[T]): Logger =
    create(tag.runtimeClass.getName, null)
  def apply[T](resource: String)(implicit tag: ClassTag[T]): Logger = {
    if (tag.runtimeClass == classOf[Nothing]) create(resource, null)
    else {
//      println("Creating logger with name "+tag.runtimeClass.getName)
      create(tag.runtimeClass.getName, resource)
    }
  }
  def apply(name: String, resource: String): Logger = create(name, resource)
  def create(name: String, resource: String = null): Logger =
    new Logger(name, resource)

}

class Logger private (name: String, resource: String) {
  val logger = JavaLogger.getLogger(name, resource)

  import scala.language.experimental.macros

  def getLevel = logger.getLevel
  def setLevel(level: java.util.logging.Level) = logger.setLevel(level)
  def getName = logger.getName
  def getResourceBundle = logger.getResourceBundle
  def getResourceBundleName = logger.getResourceBundleName

  def isLoggable(level: java.util.logging.Level) = logger.isLoggable(level)
  def log(level: java.util.logging.Level, message: String): Unit =
    macro LoggerMacro.logMessage
  def log(
      level: java.util.logging.Level,
      message: String,
      cause: Throwable
  ): Unit = macro LoggerMacro.logMessageCause
  def log(level: java.util.logging.Level, message: String, args: Any*): Unit =
    macro LoggerMacro.logMessageArgs

  def isSevereLoggable() = logger.isLoggable(java.util.logging.Level.SEVERE)
  def severe(message: String): Unit = macro LoggerMacro.severeMessage
  def severe(message: String, cause: Throwable): Unit =
    macro LoggerMacro.severeMessageCause
  def severe(message: String, args: Any*): Unit =
    macro LoggerMacro.severeMessageArgs

  def isWarningLoggable() = logger.isLoggable(java.util.logging.Level.WARNING)
  def warning(message: String): Unit = macro LoggerMacro.warningMessage
  def warning(message: String, cause: Throwable): Unit =
    macro LoggerMacro.warningMessageCause
  def warning(message: String, args: Any*): Unit =
    macro LoggerMacro.warningMessageArgs

  def isInfoLoggable() = logger.isLoggable(java.util.logging.Level.INFO)
  def info(message: String): Unit = macro LoggerMacro.infoMessage
  def info(message: String, cause: Throwable): Unit =
    macro LoggerMacro.infoMessageCause
  def info(message: String, args: Any*): Unit =
    macro LoggerMacro.infoMessageArgs

  def isConfigLoggable() = logger.isLoggable(java.util.logging.Level.CONFIG)
  def config(message: String): Unit = macro LoggerMacro.configMessage
  def config(message: String, cause: Throwable): Unit =
    macro LoggerMacro.configMessageCause
  def config(message: String, args: Any*): Unit =
    macro LoggerMacro.configMessageArgs

  def isFineLoggable() = logger.isLoggable(java.util.logging.Level.FINE)
  def fine(message: String): Unit = macro LoggerMacro.fineMessage
  def fine(message: String, cause: Throwable): Unit =
    macro LoggerMacro.fineMessageCause
  def fine(message: String, args: Any*): Unit =
    macro LoggerMacro.fineMessageArgs

  def isFinerLoggable() = logger.isLoggable(java.util.logging.Level.FINER)
  def finer(message: String): Unit = macro LoggerMacro.finerMessage
  def finer(message: String, cause: Throwable): Unit =
    macro LoggerMacro.finerMessageCause
  def finer(message: String, args: Any*): Unit =
    macro LoggerMacro.finerMessageArgs

  def isFinestLoggable() = logger.isLoggable(java.util.logging.Level.FINEST)
  def finest(message: String): Unit = macro LoggerMacro.finestMessage
  def finest(message: String, cause: Throwable): Unit =
    macro LoggerMacro.finestMessageCause
  def finest(message: String, args: Any*): Unit =
    macro LoggerMacro.finestMessageArgs

  def isEnteringLoggable() = logger.isLoggable(java.util.logging.Level.FINER)
  def entering(): Unit = macro LoggerMacro.enteringNoArgs
  def entering(args: Any*): Unit = macro LoggerMacro.enteringArgs
  def isExitingLoggable() = logger.isLoggable(java.util.logging.Level.FINER)
  def exiting(): Unit = macro LoggerMacro.exitingNoArg
  def exiting(arg: Any): Unit = macro LoggerMacro.exitingArg
  def isThrowingLoggable() = logger.isLoggable(java.util.logging.Level.FINER)
  def throwing(arg: Throwable): Unit = macro LoggerMacro.throwingArg
}

import com.github.thebridsk.utilities.macros.Source
import scala.reflect.ClassTag
import scala.reflect.ClassTag

class LoggerMacro(override val c: Context) extends Source(c) {

  def logMessage(
      level: c.Expr[java.util.logging.Level],
      message: c.Expr[String]
  ) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    q"if ($logger.isLoggable($level)) $logger.logp( $level, $cls, $method, $message)"
  }

  def logMessageCause(
      level: c.Expr[java.util.logging.Level],
      message: c.Expr[String],
      cause: c.Expr[Throwable]
  ) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    q"if ($logger.isLoggable($level)) $logger.logp( $level, $cls, $method, $message, $cause)"
  }

  def logMessageArgs(
      level: c.Expr[java.util.logging.Level],
      message: c.Expr[String],
      args: c.Expr[Any]*
  ) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    if (args.length == 1) {
      q"if ($logger.isLoggable($level)) $logger.logp( $level, $cls, $method, $message, ${args(0)} )"
    } else {
      q"if ($logger.isLoggable($level)) { $logger.logp( $level, $cls, $method, $message, Array[Object](..$args) ) }"
    }
  }

  def severeMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.SEVERE"), message)
  }

  def severeMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.SEVERE"), message, cause)
  }

  def severeMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"java.util.logging.Level.SEVERE"), message, args: _*)
  }

  def warningMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.WARNING"), message)
  }

  def warningMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.WARNING"), message, cause)
  }

  def warningMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(
      c.Expr(q"java.util.logging.Level.WARNING"),
      message,
      args: _*
    )
  }

  def infoMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.INFO"), message)
  }

  def infoMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.INFO"), message, cause)
  }

  def infoMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"java.util.logging.Level.INFO"), message, args: _*)
  }

  def configMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.CONFIG"), message)
  }

  def configMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.CONFIG"), message, cause)
  }

  def configMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"java.util.logging.Level.CONFIG"), message, args: _*)
  }

  def fineMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.FINE"), message)
  }

  def fineMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.FINE"), message, cause)
  }

  def fineMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"java.util.logging.Level.FINE"), message, args: _*)
  }

  def finerMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.FINER"), message)
  }

  def finerMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.FINER"), message, cause)
  }

  def finerMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"java.util.logging.Level.FINER"), message, args: _*)
  }

  def finestMessage(message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"java.util.logging.Level.FINEST"), message)
  }

  def finestMessageCause(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"java.util.logging.Level.FINEST"), message, cause)
  }

  def finestMessageArgs(message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"java.util.logging.Level.FINEST"), message, args: _*)
  }

  def enteringNoArgs() = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    q"""if ($logger.isLoggable(java.util.logging.Level.FINER)) $logger.entering( $cls, $method )"""
  }

  def enteringArgs(args: c.Expr[Any]*) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    if (args.length == 1) {
      q"""if ($logger.isLoggable(java.util.logging.Level.FINER)) $logger.entering( $cls, $method, ${args(
        0
      )} )"""
    } else {
      q"""if ($logger.isLoggable(java.util.logging.Level.FINER)) { $logger.entering( $cls, $method, Array[AnyRef](..$args) ) }"""
    }
  }

  def exitingNoArg() = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    q"""if ($logger.isLoggable(java.util.logging.Level.FINER)) $logger.exiting( $cls, $method )"""
  }

  def exitingArg(arg: c.Expr[Any]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    q"""if ($logger.isLoggable(java.util.logging.Level.FINER)) $logger.exiting( $cls, $method, $arg )"""
  }

  def throwingArg(arg: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    val cls = getFullClassName(getClassSymbol)
    val method = getMethodName(getMethodSymbol)
    q"""if ($logger.isLoggable(java.util.logging.Level.FINER)) $logger.throwing( $cls, $method, $arg )"""
  }

}
