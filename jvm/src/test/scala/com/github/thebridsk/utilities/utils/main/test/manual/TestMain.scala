//
// Created May 24, 2015
//
package com.github.thebridsk.utilities.main.test.manual

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Logging
import com.github.thebridsk.utilities.macros.Source._
import java.util.logging.Level
import com.github.thebridsk.utilities.main.Main
import org.rogach.scallop.ScallopConf
import com.github.thebridsk.utilities.classpath.ClassPath

class TestMacros extends Logging {

  def testMacros( x: String ) : Unit = {
    logger.info("in testMacros(x=%s)",x)
    logger.info("className is %s",className)
    logger.info("methodName is %s",methodName)
    logger.info("sourceFilename is %s",sourceFilename)
    logger.info("sourceFullFilename is %s",sourceFullFilename)
    logger.info("sourceLine is %d",sourceLine)
  }

  import scala.language.implicitConversions
  implicit def intToAnyRef( i: Int ) : AnyRef =  java.lang.Integer.valueOf(i)

  def testMacros( i: Int ) : Unit = {
    logger.info("in testMacros(x=%s)",i)
    logger.info("className is %s",className)
    logger.info("methodName is %s",methodName)
    logger.info("sourceFilename is %s",sourceFilename)
    logger.info("sourceFullFilename is %s",sourceFullFilename)
    logger.info("sourceLine is %s",sourceLine)
  }
}
/**
 * @author werewolf
 *
 */
object TestMain extends Main { // with Logging {

  val version = "0.2"

  val optionDef = toggle("def", descrYes="enable def", descrNo="disable def")
  val optionA = toggle("abc", descrYes="enable abc")
  val optionXyz = opt[String]("xyz", short='x', descr="val is the xyz option", argName="val")

  val runFunFunction = () => logger.info("Hello from runFunFunction")

  def execute() = {
    println(ClassPath.show("", getClass.getClassLoader))
    testLogging()
    if (optionXyz.isSupplied) {
      logger.info("option xyz: %s", optionXyz())
    } else {
      logger.info("option xyz is not set")
    }
    val tm = new TestMacros
    tm.testMacros(0)
    tm.testMacros("hello")
    runFun(runFunFunction)
    0
  }

  def runFun( fun: ()=>Unit) = {
    fun()
  }

  class ExceptionForTest( msg: String ) extends Exception( msg )

  def testLogging() : Unit = {
    logger.logger.setLevel(Level.FINE)

    logger.severe("hello noargs")
    logger.severe("hello %s",value())
    logger.severe("hello %s, goodbye %s",value(), "sucker")
    logger.severe("severe exception", new ExceptionForTest("oops, showing exception"))

    logger.warning("hello noargs")
    logger.warning("hello %s",value())
    logger.warning("hello %s, goodbye %s",value(), "sucker")
    logger.warning("warning exception", new ExceptionForTest("oops, showing exception"))

    logger.info("hello noargs")
    logger.info("hello %s",value())
    logger.info("hello %s, goodbye %s",value(), "sucker")
    logger.info("info exception", new ExceptionForTest("oops, showing exception"))

    logger.config("hello noargs")
    logger.config("hello %s",value())
    logger.config("hello %s, goodbye %s",value(), "sucker")
    logger.config("config exception", new ExceptionForTest("oops, showing exception"))

    logger.fine("hello noargs")
    logger.fine("hello %s",value())
    logger.fine("hello %s, goodbye %s",value(), "sucker")
    logger.fine("fine exception", new ExceptionForTest("oops, showing exception"))

    logger.finer("hello noargs")
    logger.finer("hello %s",value())
    logger.finer("hello %s, goodbye %s",value(), "sucker")
    logger.finer("finer exception", new ExceptionForTest("oops, showing exception"))

    logger.finest("hello noargs")
    logger.finest("hello %s",value())
    logger.finest("hello %s, goodbye %s",value(), "sucker")
    logger.finest("finest exception", new ExceptionForTest("oops, showing exception"))

    logger.entering()
    logger.exiting()

    logger.entering("hello")
    logger.exiting(null)

    logger.entering("hello", "world")
    logger.exiting("goodbye")

    logger.throwing(new ExceptionForTest("logging a throw of NPE"))

    TestXX.staticMethod()

  }


  def value() : String = {
    logger.info("value() called, return world")
    "world"
  }

  val tracefun = {
    logger.info("Hello from tracefun")
  }

}

object TestXX {
   private lazy val logger: Logger =
    Logger(getClass.getName)
 def staticMethod() : Unit = {
    logger.info("From staticMethod()")
  }
}
