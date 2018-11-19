
package utils.main

import utils.logging.Logger
import utils.logging.Logging

import java.util.logging.Level

import java.io.PrintWriter

import scala.util.control.Breaks._
import scala.language.postfixOps
import utils.logging.Config
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.exceptions.Help
import org.rogach.scallop.exceptions.Version
import org.rogach.scallop.exceptions.Exit
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.exceptions.RequiredOptionNotFound
import org.rogach.scallop.{Subcommand => ScallopSubcommand}
import org.rogach.scallop.Scallop
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.ScallopOption
import utils.main.logging.ConfigArguments
import utils.classpath.ClassPath
import org.rogach.scallop.ScallopConfBase
import scala.annotation.tailrec

case class ExitException( rc: Int ) extends Exception

abstract class Subcommand( val name: String, val aliases: String* ) extends ScallopSubcommand((name::aliases.toList): _*) {

  def init(): Int = 0

  def executeSubcommand(): Int

  def cleanup(): Unit = {}
}

abstract class Main( val defaultLevel: Option[Level] = None ) extends ScallopConf with Logging {

  override def onError(e: Throwable): Unit = e match {
    case Help("") =>
      builder.printHelp
      throw new ExitException(99)
    case Help(subname) =>
      builder.findSubbuilder(subname).get.printHelp
      throw new ExitException(99)
    case Version =>
      builder.vers.foreach(println)
      throw new ExitException(99)
    case ScallopException(message) =>
      if (System.console() == null) {
        // no colors on output
        println("[%s] Error: %s" format (printedName, message))
      } else {
        println("[\u001b[31m%s\u001b[0m] Error: %s" format (printedName, message))
      }
      throw new ExitException(1)

    case other => throw other
  }

  def init(): Int = 0

  def execute() : Int

  def cleanup(): Unit = {}

  def main(args: Array[String]): Unit = {
    System.exit(mainRun(args))
  }

  def mainRun(args: Array[String]): Int = {

    val loggerOptions = new ConfigArguments(this,defaultLevel)

    lazy val version = builder.vers.getOrElse("unknown")

    try {
      Config.setProgramInfo(getClass.getName, version, getClass.getClassLoader)
      Config.initialize()

      try {
        val initrc = init()
        if (initrc==0) {
          parseArgs( args ) match {
            case 0 =>
              loggerOptions.execute()

              showArgs(args)

              val rc = if (subcommands.isEmpty) {
                logger.fine("Calling execute")
                execute()
              } else {
                logger.fine("Calling execute")
                invokeSubcommand(subcommands)
              }
              rc
            case rc =>
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
        logger.severe("Uncaught exception", t)
        98
    }
  }

  private def invokeSubcommand( subc: List[ScallopConfBase] ): Int = {

    def cmdname( cmd: List[ScallopConfBase] ) = {
      cmd.reverse.map { scb =>
        if (scb.isInstanceOf[Subcommand]) scb.asInstanceOf[Subcommand].name else "unknown"
      }.mkString(" "," ","")
    }

    def asSubcommand( scb: ScallopConfBase ) = if (scb.isInstanceOf[Subcommand]) Some(scb.asInstanceOf[Subcommand]) else None

    /*
     * Initizalize the subcommands in order.  Stop initializing when one returns a non-zero value.
     * @param cmds the list of subcommands invoked
     * @param inited the subcommands that have been initialized already, head is the most recent.
     * @return tuple(rc,inited).  Where rc is the return code from a failed init() or the executeSubcommand() call,
     * and inited are the subcommands that have been initialized, head is the most recent.
     */
    @tailrec
    def init( cmds: List[ScallopConfBase], inited: List[ScallopConfBase] = List() ): (Int, List[ScallopConfBase]) = {
      if (!cmds.isEmpty) {
        val newinited = cmds.head :: inited
        val rc =
          cmds.headOption.flatMap( asSubcommand ).
                          map { sc =>
                            try {
                              sc.init()
                            } catch {
                              case x: Throwable =>
                                logger.warning(s"""Failed in init of subcommand${cmdname(inited)}""", x)
                                98
                            }
                          }.getOrElse(0)
        if (rc == 0) {
          init( cmds.tail, newinited )
        } else {
          (rc, newinited)
        }
      } else {
        val rc = inited.headOption.flatMap( asSubcommand ).
                                   map { sc =>
                                     try {
                                       sc.executeSubcommand()
                                     } catch {
                                       case x: Throwable =>
                                         logger.warning(s"""Failed in executeSubcommand of subcommand${cmdname(inited)}""", x)
                                         98
                                     }
                                   }.
                                   getOrElse( execute() )
        (rc, inited)
      }
    }

    val (rc, inited) = init(subc)

    // cleanup
    @tailrec
    def cleanup( cmds: List[ScallopConfBase] ): Unit = {
      if (!cmds.isEmpty) {
        cmds.headOption.flatMap( asSubcommand ).
                        map { sc =>
                          try {
                            sc.cleanup()
                          } catch {
                            case x: Throwable =>
                              logger.warning(s"""Failed in cleanup of subcommand${cmdname(inited)}""", x)
                          }
                        }

        cleanup( cmds.tail )
      }
    }
    cleanup(inited)

    rc
  }

  private def showArgs( args: Array[String]): Unit = {
    logger.fine( "Args:\n"+ (for (a <- args.zipWithIndex) yield { a._2.toString()+": "+a._1 }).mkString("\n") )
  }

  private def parseArgs( args: Array[String]): Int = {
    try {
      editBuilder { _.args(args) }
      verify
      showArgs(args)
      0
    } catch {
      case ExitException(rc) => rc
    }
  }
}
