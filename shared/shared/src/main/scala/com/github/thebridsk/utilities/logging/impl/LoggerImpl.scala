package com.github.thebridsk.utilities.logging.impl

import com.github.thebridsk.utilities.logging.LoggerFactory
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.TraceMsg

import com.github.thebridsk.utilities.logging.Handler
import com.github.thebridsk.utilities.logging.Level

trait LoggerImplFactory extends LoggerFactory {

  def doingReplacement = {
    var rc = false
    if (LoggerFactory.factory != null && LoggerFactory.factory != this) {
      if (LoggerFactory.factory.isReplaceable) {
        if (LoggerFactory.factory.isInstanceOf[LoggerImplFactory]) {
          rc = true
        }
      }
    }
    rc
  }

  def init(rootHandler: Handler*) = {
    if (LoggerFactory.factory != null && LoggerFactory.factory != this) {
      if (!LoggerFactory.factory.isReplaceable) {
        throw new IllegalStateException("Logger.factory is not replaceable")
      }
      if (LoggerFactory.factory.isInstanceOf[LoggerImplFactory]) {
        throw new IllegalStateException(
          "Replacement Logger.factory must extend LoggerImplFactory"
        )
      }
    }
    LoggerFactory.factory = this
    rootHandler.foreach(h => rootLogger.addHandler(h))
  }

  import scala.language.postfixOps
  val hierarchySearch = """(.+)\.([^.]+)""" r

  /**
    * Get the logger
    * @param name the name of the logger.  The "." is a hierachy separator in the name.
    * @param resource the resource bundle name to set in the logger, if the logger does not have a bundle.
    */
  def getLogger(name: String, resource: String = null): Logger = {
    if (name == "") rootLogger
    else {
      name match {
        case hierarchySearch(path, n) =>
          val parent = getLogger(path, null)
          getLoggerPrivate(name, Some(parent), resource)
        case _ =>
          getLoggerPrivate(name, Some(rootLogger), resource)
      }
    }
  }

  private def getLoggerPrivate(
      name: String,
      parent: Option[Logger],
      resource: String
  ) = {
    allLoggers.get(name) match {
      case Some(l) =>
        if (resource != null && l.resource == null) l.resource = resource
        l
      case None =>
        val l = new LoggerImpl(name, parent, resource)
        allLoggers += (name -> l)
        l
    }
  }

  private val rootLogger: LoggerImpl = {
    if (doingReplacement)
      LoggerFactory.factory.asInstanceOf[LoggerImplFactory].rootLogger
    else {
      val l = new LoggerImpl("", None)
      l.setLevel(Level.INFO)
      l
    }
  }
  private val allLoggers: collection.mutable.Map[String, LoggerImpl] = {
    if (doingReplacement) {
      LoggerFactory.factory.asInstanceOf[LoggerImplFactory].allLoggers
    } else {
      collection.mutable.Map[String, LoggerImpl]("" -> rootLogger)
    }
  }
}

object LoggerImplFactory extends LoggerImplFactory {
  override def isReplaceable = true

  private var systemTime: SystemTime = null

  def setSystemTimeObject(st: SystemTime) = systemTime = st

  def getTime(): Double = systemTime.currentTimeMillis()

  def formatTime(time: Double) = systemTime.formatTime(time)
}

trait SystemTime {

  /**
    * returns the time in milliseconds since 1/1/1970
    */
  def currentTimeMillis(): Double

  /**
    * @param time the time in milliseconds since 1/1/1970
    * @return the returned string has the format HH:mm:ss.SSS
    */
  def formatTime(time: Double): String
}

class LoggerImpl(
    name: String,
    parent: Option[Logger],
    var resource: String = null
) extends Logger(name, parent) {

  def getTime: Double = LoggerImplFactory.getTime()

  def logImpl(traceMsg: TraceMsg): Unit = {
    if (isLoggable(traceMsg.level)) {
      logToHandlersAndParent(traceMsg)
    }
  }

}
