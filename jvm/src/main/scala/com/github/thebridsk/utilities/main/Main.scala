package com.github.thebridsk.utilities.main

import com.github.thebridsk.utilities.logging.Logging

import java.util.logging.Level

import com.github.thebridsk.utilities.logging.Config
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.{Subcommand => ScallopSubcommand}
import com.github.thebridsk.utilities.main.logging.ConfigArguments
import org.rogach.scallop.ScallopConfBase
import scala.annotation.tailrec
import com.github.thebridsk.utilities.logging.Logger
import java.io.StringWriter
import java.io.PrintWriter
import scala.reflect.ClassTag
import org.rogach.scallop.exceptions.ScallopResult
import org.rogach.scallop.exceptions.Help
import org.rogach.scallop.exceptions.Version
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.Util

object Main {
  private[main] val log: Logger = Logger[Main[_]]()

  /**
   * Used to pass the arguments to the ScalaConf object
   */
  private[main] val args = new ThreadLocal[Array[String]]

  class ExitMain(val status: Int) extends RuntimeException(s"ExitMain status $status")
}

import Main.log

/**
  * Configuration and execution engine for a subcommand.
  *
  * See the ScallopConfBase class on defining options.
  *
  * The following methods are invoked in order:
  * - init perform initialization.
  * - executeSubcommand - execute the command.
  * - cleanup - always called if init call was attempted.
  *
  * Subcommands can be nested.  If the nested command is executed,
  * the following methods will be invoked:
  * 1. outer.init
  * 2.   nested.init
  * 3.   nested.executeSubcommand
  * 4.   nested.cleanup
  * 5. outer.cleanup
  *
  * Usage:
  * {{{
  * class NestedSubcommand extends Subcommand("d") {
  *   def executeSubcommand(): Int = {
  *     0
  *   }
  * }
  * class MySubcommand extends Subcommand("b") {
  *
  *   val optC = opt[Int]("c")
  *   addSubcommand(NestedSubcommand)
  *
  *   def executeSubcommand(): Int = {
  *     optC()
  *     0
  *   }
  * }
  * }}}
  *
  * @param name - the main name used to invoke the subcommand.
  * @param aliases - aliases that can be used to invoke the subcommand.
  *
  */
abstract class Subcommand(val name: String, val aliases: String*)
    extends ScallopSubcommand((name :: aliases.toList): _*) {

  /**
    * Called to initialize, before executing the command.
    *
    * Argument values are available for use.
    *
    * Default implementation just returns 0, success.
    *
    * @return status code.  0 is success.  Anything else cause program to terminate.
    */
  def init(): Int = 0

  /**
    * Called to execute the command.
    *
    * @return status code.  0 is success.  This becomes the exit status of the program.
    */
  def executeSubcommand(): Int

  /**
   * Called to cleanup any resource that were initialized or used.
   *
   * Always called if [[init]] was called.
   */
  def cleanup(): Unit = {}
}

/**
  * Base class for command line parsing definitions
  * when using [[Main!]].
  *
  * This class extends ScallopConf and supplies the arguments to ScallopConf.
  *
  * This class MUST be used with [[Main!]].
  *
  * Usage:
  * {{{
  * object SubcommandB extends Subcommand("b") {
  *   def execute(): Int = {
  *     0
  *   }
  * }
  *
  * class MyConf extends MainConf {
  *   val optA = opt[Int]("a", ...)
  *
  *   addSubcommand(SubcommandB)
  * }
  * }}}
  *
  * See the methods on ScallopConf on defining the command line arguments.
  */
class MainConf
    extends ScallopConf(Main.args.get) {

  private var loggerOptions: ConfigArguments = null

  private[main] def mainInit(defaultLevel: Option[Level] = None): Unit = {
    log.fine(s"Setting up logger config with default level ${defaultLevel}")
    loggerOptions = new ConfigArguments(this, defaultLevel)
  }

  private[main] def mainExecute(): Unit = {
    log.fine("Setting up logging based on arguments")
    loggerOptions.execute()
  }

  errorMessageHandler = { message =>
    Console.err.println(Util.format("[%s] Error: %s", printedName, message))
    throw new Main.ExitMain(1)
  }

  override protected def onError(e: Throwable): Unit = {
    e match {
      case r: ScallopResult =>
        r match {
          case Help("") =>
            builder.printHelp()
            throw new Main.ExitMain(99)
          case Help(subname) =>
            builder.findSubbuilder(subname).get.printHelp()
            throw new Main.ExitMain(99)
          case Version =>
            builder.vers.foreach(println)
            throw new Main.ExitMain(99)
          case x: ScallopException =>
            errorMessageHandler(x.message)
        }
      case e => throw e
    }
  }
}

/**
  * Base class for defining main programs with logging and command line parsing.
  *
  * Logging is setup by loading a logging.properties file from the following:
  * 1. current directory
  * 2. logging.properties with the resource path with the same name as the main's package
  * 3. the resource path com.github.thebridsk.utilities/logging/logging.properties
  *
  * Command line options are also added for logging, that can be used to configure logging
  * at command execution.
  *
  * The following methods are called in order:
  * - [[init]] initialization that must be done before configuration is defined
  * - [[conf]] create configuration object.  Default is to instantiate T with
  *            the constructor that takes no arguments.
  * - [[config]] the configuration object, can use {{{import config._}}}
  *              This val must not be accessed prior to the return of the
  *              [[conf]] method.
  * - [[setup]] called after the configuration has parsed the arguments
  * - [[execute]] the main code
  * - [[cleanup]] called at the conclusion of the execute method,
  *               no matter how the execute method returns.
  *
  * Usage:
  * {{{
  * class MyConf extends MainConf {
  *   val optA = opt[Int]("a")
  * }
  *
  * object MyMain extends Main[MyConf] {
  *   import config._
  *   def execute(): Int = {
  *     optA()
  *   }
  * }
  * }}}
  *
  * If no configuration object is required, then the [[MainNoArgs]] base class
  * should be used instead of [[Main!]]
  *
  * @tparam T they type of the command line configuration class.
  *
  * @see See [[MainConf!]] for command line definitions.
  *
  * @constructor
  * @param defaultLevel
  */
abstract class Main[T <: MainConf : ClassTag](
    val defaultLevel: Option[Level] = None
) extends Logging {

  /**
    * The version for the program
    */
  val version: String = "unknown"

  private var pconfig: Option[T] = None

  lazy val config: T = pconfig.get

  /**
    * Called at startup, before arguments are parsed.
    * @return status code.  0 is success, anything else
    *         causes program to terminate.
    */
  def init(): Int = 0

  /**
    * Called after [[init]] to obtain and parse the arguments.
    *
    * The default implementation instantiates the configuration class {{{T}}}
    * using the constructor with no arguments with code.
    *
    * @return the configuration object
    */
  def conf(): T =  {
    val cls = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    cls.getConstructor().newInstance()
  }

  /**
    * Called after arguments are parsed, before execute.
    * @return status code.  0 is success, anything else
    *         causes program to terminate with code.
    */
  def setup(): Int = 0

  /**
    * Execute top level command, this is not called for subcommands.
    * @return status code.  0 is success, this becomes the exit status of program.
    */
  def execute(): Int

  /**
    * called just before shutting down JVM.
    * Always called.
    */
  def cleanup(): Unit = {}

  /**
    * Main when starting from command line.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    System.exit(mainRun(args))
  }

  /**
    * Run the main program.  This will not cause the JVM to exit.
    *
    * @param args
    * @return the exit status
    */
  def mainRun(args: Array[String]): Int = {

    try {
      Config.setProgramInfo(getClass.getName, version, getClass.getClassLoader)
      Config.initialize()

      try {
        val initrc = init()
        if (initrc == 0) {
          parseArgs(args) match {
            case Right(c) =>

              showArgs(args)

              val setuprc = setup()
              if (setuprc == 0) {
                val rc = if (c.subcommands.isEmpty) {
                  logger.fine("Calling execute")
                  execute()
                } else {
                  logger.fine("Calling execute")
                  invokeSubcommand(c.subcommands)
                }
                rc
              } else {
                setuprc
              }

            case Left(rc) =>
              rc
          }
        } else {
          initrc
        }
      } finally {
        cleanup()
      }
    } catch {
      case t: Throwable =>
        val sw = new StringWriter
        t.printStackTrace(new PrintWriter(sw))
        val st = sw.toString()
        // println(st)
        logger.severe(s"Uncaught exception:\n${st}")
        98
    }
  }

  private def invokeSubcommand(subc: List[ScallopConfBase]): Int = {

    def cmdname(cmd: List[ScallopConfBase]) = {
      cmd.reverse
        .map { scb =>
          if (scb.isInstanceOf[Subcommand]) scb.asInstanceOf[Subcommand].name
          else "unknown"
        }
        .mkString(" ", " ", "")
    }

    def asSubcommand(scb: ScallopConfBase) =
      if (scb.isInstanceOf[Subcommand]) Some(scb.asInstanceOf[Subcommand])
      else None

    /*
     * Initizalize the subcommands in order.  Stop initializing when one returns a non-zero value.
     * @param cmds the list of subcommands invoked
     * @param inited the subcommands that have been initialized already, head is the most recent.
     * @return tuple(rc,inited).  Where rc is the return code from a failed init() or the executeSubcommand() call,
     * and inited are the subcommands that have been initialized, head is the most recent.
     */
    @tailrec
    def init(
        cmds: List[ScallopConfBase],
        inited: List[ScallopConfBase] = List()
    ): (Int, List[ScallopConfBase]) = {
      if (!cmds.isEmpty) {
        val newinited = cmds.head :: inited
        val rc =
          cmds.headOption
            .flatMap(asSubcommand)
            .map { sc =>
              try {
                sc.init()
              } catch {
                case x: Throwable =>
                  logger.warning(
                    s"""Failed in init of subcommand${cmdname(
                      inited
                    )}""",
                    x
                  )
                  98
              }
            }
            .getOrElse(0)
        if (rc == 0) {
          init(cmds.tail, newinited)
        } else {
          (rc, newinited)
        }
      } else {
        val rc = inited.headOption
          .flatMap(asSubcommand)
          .map { sc =>
            try {
              sc.executeSubcommand()
            } catch {
              case x: Throwable =>
                logger.warning(
                  s"""Failed in executeSubcommand of subcommand${cmdname(
                    inited
                  )}""",
                  x
                )
                98
            }
          }
          .getOrElse(execute())
        (rc, inited)
      }
    }

    val (rc, inited) = init(subc)

    // cleanup
    @tailrec
    def cleanup(cmds: List[ScallopConfBase]): Unit = {
      if (!cmds.isEmpty) {
        cmds.headOption.flatMap(asSubcommand).map { sc =>
          try {
            sc.cleanup()
          } catch {
            case x: Throwable =>
              logger.warning(
                s"""Failed in cleanup of subcommand${cmdname(
                  inited
                )}""",
                x
              )
          }
        }

        cleanup(cmds.tail)
      }
    }
    cleanup(inited)

    rc
  }

  private def showArgs(args: Array[String]): Unit = {
    logger.fine("Args:\n" + (for (a <- args.zipWithIndex) yield {
      a._2.toString() + ": " + a._1
    }).mkString("\n"))
  }

  private def parseArgs(args: Array[String]): Either[Int, MainConf] = {
    try {
      Main.args.set(args)
      val c = conf()
      c.version(version)
      c.mainInit(defaultLevel)
      c.verify()
      c.mainExecute()
      pconfig = Some(c)
      Right(c)
    } catch {
      case x: Main.ExitMain =>
        Left(x.status)
      case x: Exception =>
        logger.severe("Error parsing the arguments", x)
        Left(1)
    }
  }
}

/**
  * Base class to main programs that don't add any command line arguments.
  *
  * There will still be command line arguments for the program to setup logging.
  *
  * @see [[Main!]] for details.
  */
abstract class MainNoArgs extends Main[MainConf] {
  override def conf(): MainConf = new MainConf
}
