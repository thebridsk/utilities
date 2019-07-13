package com.github.thebridsk.utilities.main.logging

import org.rogach.scallop.ScallopConf
import java.io.File
import java.util.logging.Level
import org.rogach.scallop.ValueConverter
import com.github.thebridsk.utilities.logging.Config

import com.github.thebridsk.utilities.main.Converters._

class ConfigArguments(
    val cmdline: ScallopConf,
    val defaultLevel: Option[Level] = None
) {

  implicit object LevelConverter extends ValueConverter[Level] {
    // parse is a method, that takes a list of arguments to all option invokations:
    // for example, "-a 1 2 -a 3 4 5" would produce List(List(1,2),List(3,4,5)).
    // parse returns Left with error message, if there was an error while parsing
    // if no option was found, it returns Right(None)
    // and if option was found, it returns Right(...)
    def parse(s: List[(String, List[String])]): Either[String, Option[Level]] =
      s match {
        case (option, level :: Nil) :: Nil =>
          try {
            Right(Some(Level.parse(level)))
          } catch {
            case e: IllegalArgumentException =>
              Left("log level value is not valid: " + level)
          }
        case Nil => Right(None) // no person found
        case _   => Left("provide log level") // error when parsing
      }

    val tag = scala.reflect.runtime.universe.typeTag[Level] // some magic to make typing work
    val argType = org.rogach.scallop.ArgType.SINGLE
  }

  import cmdline._
  val logfile = opt[String](
    "logfile",
    noshort = true,
    descr = "log filename pattern",
    argName = "filename",
    default = None
  )
  val logconsolelevel = opt[Level](
    "logconsolelevel",
    noshort = true,
    descr = "The level to use for console logging",
    argName = "level",
    default = defaultLevel
  )

  def execute() = {
    logfile.foreach { filename =>
      Config.setLogFile(filename)
    }
    logconsolelevel.foreach { level =>
      Config.setLevelOnConsoleHandler(level)
    }
  }

}
