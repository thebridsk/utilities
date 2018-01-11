package utils.logging

import language.experimental.macros
import java.util.logging.{ Logger => JavaLogger }
import scala.reflect.macros.blackbox.Context
import scala.reflect.ClassTag
import org.scalactic.source.Position
import utils.logging.impl.LoggerImplFactory
import scala.annotation.tailrec

trait Logging {
  val resource : String = null
  lazy val logger = Logger.create( getClass().getName, resource)
}

trait LoggerFactory {
  def isReplaceable = false
  def getLogger( name: String, resource: String = null) : Logger
}

object LoggerFactory {
  @volatile var factory: LoggerFactory = null
}

object Logger {

  def apply[T]()(implicit tag: ClassTag[T] ) : Logger = create( tag.runtimeClass.getName, null )
  def apply[T]( resource: String )(implicit tag: ClassTag[T] ) : Logger = {
    if (tag.runtimeClass == classOf[Nothing]) create(resource,null)
    else create( tag.runtimeClass.getName, resource )
  }
  def apply( name: String, resource: String ): Logger = create(name,resource)
  def create( name: String, resource: String = null) : Logger = {
    import LoggerFactory._
    if (factory == null) LoggerImplFactory.init()
    factory.getLogger( name, resource )
  }

}

abstract class Logger( val name: String, val parent: Option[Logger] ) {

  import scala.language.experimental.macros

  private val propogate: Boolean = true
  private final var level: Option[Level] = None

  final def getLevel = level
  final def setLevel(l: Level ) = level = Some(l)
  final def resetLevel() = level = None

  private final def effectiveLevel: Level = // Level.INFO
  {
    getEffectiveLevel
  }

  /**
   * Called by framework to set the effective level
   */
  final def setEffectiveLevel = {
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

  def addHandler( handler: Handler ) = handlers = handler::handlers
  def removeHandler( handler: Handler ) = handlers = handlers.filter(_ != handler)
  @volatile
  def getHandlers() = handlers

  @tailrec
  protected final def logToHandlersAndParent( traceMsg: TraceMsg ): Unit = {
    handlers.filter{ _.isLoggingLevel(traceMsg.level) }.foreach(_.log(traceMsg))
    if (propogate) parent match {
      case Some(p) => p.logToHandlersAndParent(traceMsg)
      case None =>
    }
  }

  @inline
  final def isLoggable(level: _root_.utils.logging.Level) = level.isLoggable(effectiveLevel)

  def log(level: _root_.utils.logging.Level, message: String): Unit = macro LoggerMacro.logMessage
  def log(level: _root_.utils.logging.Level, message: String, cause: Throwable): Unit = macro LoggerMacro.logMessageCause
  def log(level: _root_.utils.logging.Level, message: String, args: Any*): Unit = macro LoggerMacro.logMessageArgs

  def isSevereLoggable() = isLoggable(_root_.utils.logging.Level.SEVERE)
  def severe( message: String): Unit = macro LoggerMacro.severeMessage
  def severe( message: String, cause: Throwable): Unit = macro LoggerMacro.severeMessageCause
  def severe( message: String, args: Any*): Unit = macro LoggerMacro.severeMessageArgs

  def isWarningLoggable() = isLoggable(_root_.utils.logging.Level.WARNING)
  def warning( message: String): Unit = macro LoggerMacro.warningMessage
  def warning( message: String, cause: Throwable): Unit = macro LoggerMacro.warningMessageCause
  def warning( message: String, args: Any*): Unit = macro LoggerMacro.warningMessageArgs

  def isInfoLoggable() = isLoggable(_root_.utils.logging.Level.INFO)
  def info( message: String): Unit = macro LoggerMacro.infoMessage
  def info( message: String, cause: Throwable): Unit = macro LoggerMacro.infoMessageCause
  def info( message: String, args: Any*): Unit = macro LoggerMacro.infoMessageArgs

  def isConfigLoggable() = isLoggable(_root_.utils.logging.Level.CONFIG)
  def config( message: String): Unit = macro LoggerMacro.configMessage
  def config( message: String, cause: Throwable): Unit = macro LoggerMacro.configMessageCause
  def config( message: String, args: Any*): Unit = macro LoggerMacro.configMessageArgs

  def isFineLoggable() = isLoggable(_root_.utils.logging.Level.FINE)
  def fine( message: String): Unit = macro LoggerMacro.fineMessage
  def fine( message: String, cause: Throwable): Unit = macro LoggerMacro.fineMessageCause
  def fine( message: String, args: Any*): Unit = macro LoggerMacro.fineMessageArgs

  def isFinerLoggable() = isLoggable(_root_.utils.logging.Level.FINER)
  def finer( message: String): Unit = macro LoggerMacro.finerMessage
  def finer( message: String, cause: Throwable): Unit = macro LoggerMacro.finerMessageCause
  def finer( message: String, args: Any*): Unit = macro LoggerMacro.finerMessageArgs

  def isFinestLoggable() = isLoggable(_root_.utils.logging.Level.FINEST)
  def finest( message: String): Unit = macro LoggerMacro.finestMessage
  def finest( message: String, cause: Throwable): Unit = macro LoggerMacro.finestMessageCause
  def finest( message: String, args: Any*): Unit = macro LoggerMacro.finestMessageArgs

  def isEnteringLoggable() = isLoggable(_root_.utils.logging.Level.FINER)
  def entering(): Unit = macro LoggerMacro.enteringNoArgs
  def entering( args: Any* ): Unit = macro LoggerMacro.enteringArgs

  def isExitingLoggable() = isLoggable(_root_.utils.logging.Level.FINER)
  def exiting(): Unit = macro LoggerMacro.exitingNoArg
  def exiting( arg: Any ): Unit = macro LoggerMacro.exitingArg

  def isThrowingLoggable() = isLoggable(_root_.utils.logging.Level.FINER)
  def throwing( ex: Throwable ): Unit = macro LoggerMacro.throwingArg

  def logImpl( traceMsg: TraceMsg ): Unit

  def getTime(): Double
}

sealed trait TraceType {
  override
  def toString() = getClass.getSimpleName
}
object LogMsgType extends TraceType
object LogEnterType extends TraceType
object LogExitType extends TraceType
object LogThrowingType extends TraceType

case class TraceMsg( val time: Double,
                     val logger: String,
                     val msgtype: TraceType,
                     val level: _root_.utils.logging.Level,
                     val message: String=null,
                     val cause: Throwable = null
                   )( val args: Any* )(implicit val pos: Position) {
  override
  def toString() = {
    s"""TraceMsg($msgtype,$level,"$message",$cause,${pos.fileName}:${pos.lineNumber}"""+args.mkString(",", ",", ")")
  }
}

import utils.macros.Source
import scala.reflect.ClassTag
import scala.reflect.ClassTag
import org.scalactic.source.Position
import utils.logging.impl.LoggerImplFactory
import utils.logging.impl.LoggerImplFactory

class LoggerMacro(override val c: Context) extends Source(c) {

  def logMessage(level : c.Expr[_root_.utils.logging.Level], message: c.Expr[String]) = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable($level)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogMsgType, $level, $message)())
        }}"""
  }

  def logMessageCause(level : c.Expr[_root_.utils.logging.Level], message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable($level)) {
            ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogMsgType, $level, $message, $cause)())
        }}"""
  }

  def logMessageArgs(level : c.Expr[_root_.utils.logging.Level], message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable($level)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogMsgType, $level, $message)( ..$args) )
        }}"""
  }

  def severeMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.SEVERE"), message)
  }

  def severeMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.SEVERE"), message, cause)
  }

  def severeMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.SEVERE"), message, args:_*)
  }

  def warningMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.WARNING"), message)
  }

  def warningMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.WARNING"), message, cause)
  }

  def warningMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.WARNING"), message, args:_*)
  }

  def infoMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.INFO"), message)
  }

  def infoMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.INFO"), message, cause)
  }

  def infoMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.INFO"), message, args:_*)
  }

  def configMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.CONFIG"), message)
  }

  def configMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.CONFIG"), message, cause)
  }

  def configMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.CONFIG"), message, args:_*)
  }

  def fineMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.FINE"), message)
  }

  def fineMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.FINE"), message, cause)
  }

  def fineMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.FINE"), message, args:_*)
  }

  def finerMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.FINER"), message)
  }

  def finerMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.FINER"), message, cause)
  }

  def finerMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.FINER"), message, args:_*)
  }

  def finestMessage( message: c.Expr[String]) = {
    import c.universe._
    logMessage(c.Expr(q"_root_.utils.logging.Level.FINEST"), message)
  }

  def finestMessageCause( message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    logMessageCause(c.Expr(q"_root_.utils.logging.Level.FINEST"), message, cause)
  }

  def finestMessageArgs( message: c.Expr[String], args: c.Expr[Any]*) = {
    import c.universe._
    logMessageArgs(c.Expr(q"_root_.utils.logging.Level.FINEST"), message, args:_*)
  }

  def enteringNoArgs() = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(_root_.utils.logging.Level.FINER)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogEnterType, _root_.utils.logging.Level.FINER)() )
        }}"""
  }

  def enteringArgs( args: c.Expr[Any]*) = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(_root_.utils.logging.Level.FINER)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogEnterType, _root_.utils.logging.Level.FINER)(..$args) )
        }}"""
  }

  def exitingNoArg() = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(_root_.utils.logging.Level.FINER)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogExitType, _root_.utils.logging.Level.FINER)() )
        }}"""
  }

  def exitingArg( arg: c.Expr[Any]) = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(_root_.utils.logging.Level.FINER)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogExitType, _root_.utils.logging.Level.FINER)($arg) )
        }}"""
  }

  def throwingArg( ex: c.Expr[Throwable]) = {
    import c.universe._
    q"""{if (${c.prefix}.isLoggable(_root_.utils.logging.Level.FINER)) {
           ${c.prefix}.logImpl( _root_.utils.logging.TraceMsg( ${c.prefix}.getTime, ${c.prefix}.name, _root_.utils.logging.LogThrowingType, _root_.utils.logging.Level.FINER, null, $ex)() )
        }}"""
  }

}

