package com.github.thebridsk.utilities.message

import java.util.logging.Logger
import org.scalactic.source.Position
import java.util.Locale
import java.util.ResourceBundle
import java.util.ResourceBundle.Control
import java.util.Formatter
import java.util.MissingResourceException
import java.util.logging.Level
import com.github.thebridsk.utilities.nls.Messages
import scala.util.Using

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
  override def toString(): String = {
    val loc = created.fileName + ":" + created.lineNumber

    "{Message bundle=" + bundle + ", key=" + key + ", args=" +
      args.mkString("(", ", ", ")") + ", loc=" + loc + "}";
  }

  /**
    * Get the message using the specified resolver.
    * @param resolver
    * @return the message
    */
  def toNLS(implicit resolver: MessageResolver): String = resolver.toNLS(this)

  /**
    * Trace the message
    * @param logger
    * @param level
    * @param clsname
    * @param methodname
    */
  def log(logger: Logger, level: Level, clsname: String, methodname: String)(
      implicit resolver: MessageResolver
  ): Unit = {
    resolver.log(logger, level, clsname, methodname, this)
  }
}

/**
  * @constructor
  * @param locale the locale to use, may not be null
  * @param loader the class loader to use, may be null for the classloader that loaded this class
  * @param control the control, may be null for the default control
  * @author werewolf
  *
  */
case class MessageResolver(
    locale: Locale = Locale.getDefault,
    loader: ClassLoader = null,
    control: Control = null
) {

  private def rb(msg: Message) = {
    val useClassLoader = Option(loader).getOrElse( getClass.getClassLoader )
    if (control == null)
      ResourceBundle.getBundle(msg.bundle, locale, useClassLoader)
    else ResourceBundle.getBundle(msg.bundle, locale, useClassLoader, control)
  }

  def toNLS(msg: Message): String = {
    Message.getNLSMessage(rb(msg), msg.bundle, locale, msg.key, msg.args: _*)
  }

  /**
    * Trace the message
    * @param logger
    * @param level
    * @param clsname
    * @param methodname
    */
  def log(
      logger: Logger,
      level: Level,
      clsname: String,
      methodname: String,
      msg: Message
  ): Unit = {
    logger.logrb(level, clsname, methodname, rb(msg), msg.key, msg.args);
  }

}

object Message {
  val fsLog: Logger =
    Logger.getLogger(classOf[Message].getName(), null /* resource bundle */ );

  /**
    * @param rb
    * @param bundleName
    * @param locale
    * @param key
    * @param args
    * @return the NLS message
    */
  def getNLSMessage(
      rb: ResourceBundle,
      bundleName: String,
      locale: Locale,
      key: String,
      args: Any*
  ): String = {
    try {
      val fmt = rb.getString(key)
      val msg = getFormattedMessage(locale, fmt, args: _*)
      //            println("formatted: "+msg)
      msg
    } catch {
      case e: MissingResourceException =>
        s"""!${bundleName},${key}${args.mkString("(", ",", ")")}!"""
    }
  }

  /**
    * @param locale
    * @param fmt
    * @param args
    * @return the formatted message
    */
  def getFormattedMessage(locale: Locale, fmt: String, args: Any*): String = {

    if (args != null && args.length > 0) {
      val b: Appendable = new java.lang.StringBuilder();
      Using.resource(new Formatter(b, locale)) { f =>
        val a = args.asInstanceOf[Seq[Object]]
        f.format(fmt, a: _*);
      }
      b.toString();
    } else {
      fmt
    }
  }

  /**
    * @param args
    */
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      System.out.println(
        "Syntax: " + classOf[Message].getName() + " locale key [args]"
      );
      System.out.println("Where:");
      System.out.println(
        "  locale is the locale to use, default: \"-\", default locale"
      );
      System.out.println("  key is a key in util.nls.messages.properties");
      System.out.println(
        "  args are the arguments to message (only String args are supported)"
      );
      System.out.println("Locales:");
      for (l <- Locale.getAvailableLocales()) {
        System.out.println("  " + l.toLanguageTag() + " " + l.getDisplayName());
      }
    }

    val slocale = args(0);
    val locale =
      if (!slocale.equals("-")) {
        val l = Locale.forLanguageTag(slocale);
        System.out.println("Using locale " + l);
        l
      } else {
        val l = Locale.getDefault
        System.out.println("Using default locale: " + l);
        null
      }
    val key = args(1);
    val as = args.drop(2)
    val m = new Message(Messages.BUNDLE_NAME, key, as.toIndexedSeq: _*);
    implicit val resolver =
      if (locale == null) MessageResolver()
      else MessageResolver(locale = locale)
    System.out.println("toString(): " + m);
    if (locale != null) {
      System.out.println("toNLS(" + locale + "): " + m.toNLS);
    } else {
      System.out.println("toNLS(defaultlocale): " + m.toNLS);
    }
  }

}
