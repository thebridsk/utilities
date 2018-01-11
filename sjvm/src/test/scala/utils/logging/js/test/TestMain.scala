//
// Created May 24, 2015
//
package utils.logging.js.test

import utils.logging.Logger
import utils.logging.Level
import org.scalactic.source.Position
import utils.logging.impl.LoggerImplFactory
import utils.logging.PrintHandler

/** @author werewolf
 *
 */
object TestMain {

  LoggerImplFactory.init()
  LoggerImplFactory.getLogger("").addHandler(new PrintHandler)
  LoggerImplFactory.getLogger("").setLevel(Level.ALL)

  val logger = Logger( "TestMain" )

  def main(args: Array[String]): Unit = {
    testLogging()
  }

  def testLogging(): Unit = {
    logger.setLevel( Level.ALL )

    logger.severe( "hello noargs" )
    logger.severe( "hello %s", value() )
    logger.severe( "hello %s, goodbye %s", value(), "sucker" )
    logger.severe( "severe exception", new NullPointerException( "oops, showing exception" ) )

    logger.warning( "hello noargs" )
    logger.warning( "hello %s", value() )
    logger.warning( "hello %s, goodbye %s", value(), "sucker" )
    logger.warning( "warning exception", new NullPointerException( "oops, showing exception" ) )

    logger.info( "hello noargs" )
    logger.info( "hello %s", value() )
    logger.info( "hello %s, goodbye %s", value(), "sucker" )
    logger.info( "info exception", new NullPointerException( "oops, showing exception" ) )

    logger.config( "hello noargs" )
    logger.config( "hello %s", value() )
    logger.config( "hello %s, goodbye %s", value(), "sucker" )
    logger.config( "config exception", new NullPointerException( "oops, showing exception" ) )

    logger.fine( "hello noargs" )
    logger.fine( "hello %s", value() )
    logger.fine( "hello %s, goodbye %s", value(), "sucker" )
    logger.fine( "fine exception", new NullPointerException( "oops, showing exception" ) )

    logger.finer( "hello noargs" )
    logger.finer( "hello %s", value() )
    logger.finer( "hello %s, goodbye %s", value(), "sucker" )
    logger.finer( "finer exception", new NullPointerException( "oops, showing exception" ) )

    logger.finest( "hello noargs" )
    logger.finest( "hello %s", value() )
    logger.finest( "hello %s, goodbye %s", value(), "sucker" )
    logger.finest( "finest exception", new NullPointerException( "oops, showing exception" ) )

    logger.entering()
    logger.exiting()

    logger.entering( "hello" )
    logger.exiting( null )

    logger.entering( "hello", "world" )
    logger.exiting( "goodbye" )

    logger.throwing( new NullPointerException( "logging a throw of NPE" ) )

    TestXX.staticMethod()

    value()
    tracefun

  }

  def value(): String = {
    logger.info( "value() called, return world" )
    "world"
  }

  val tracefun = {
    logger.info( "Hello from tracefun" )
  }

}

object TestXX {
  private lazy val logger: Logger =
    Logger( getClass.getName )
  def staticMethod(): Unit = {
    logger.info( "From staticMethod()" )
  }
}
