package com.github.thebridsk.utilities.message

import org.scalactic.source.Position
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Level

/**
  * @constructor
  * @param bundle
  * @param key
  * @param args
  */
case class Message(bundle: String, key: String, args: Any*)(
    implicit created: Position
) {

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = {
    val loc = created.fileName + ":" + created.lineNumber

    "{Message bundle=" + bundle + ", key=" + key + ", args=" +
      args.mkString("(", ", ", ")") + ", loc=" + loc + "}";
  }

  /**
    * Get the message using the specified resolver.
    * @param resolver
    * @param translated - where the msg was converted to string
    * @return the message
    */
  def toNLS()(implicit resolver: MessageResolver, translated: Position) = {
    resolver.toNLS(this)(translated)
  }

  /**
    * Trace the message
    * @param logger
    * @param level
    * @param resolver
    * @param logged - where the msg was logged
    */
  def log(
      logger: Logger,
      level: Level
  )(implicit resolver: MessageResolver, logged: Position) = {
    resolver.log(logger, level, this)(logged)
  }
}

/**
  * @author werewolf
  *
  */
trait MessageResolver {

  /**
    * Convert the message to a string using the locale of the resolver.
    */
  def toNLS(msg: Message)(implicit translated: Position): Unit

  /**
    * Trace the message
    * @param logger
    * @param level
    * @param msg
    * @param logged - where the msg was logged
    */
  def log(logger: Logger, level: Level, msg: Message)(implicit logged: Position): Unit

}
