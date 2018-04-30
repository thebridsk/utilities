package utils.main

import org.rogach.scallop.ValueConverter
import scala.reflect.io.Path
import java.io.File
import scala.reflect.io.Directory

object Converters {
  implicit object PathConverter extends ValueConverter[Path] {
    // parse is a method, that takes a list of arguments to all option invokations:
    // for example, "-a 1 2 -a 3 4 5" would produce List(List(1,2),List(3,4,5)).
    // parse returns Left with error message, if there was an error while parsing
    // if no option was found, it returns Right(None)
    // and if option was found, it returns Right(...)
    def parse(s:List[(String, List[String])]):Either[String,Option[Path]] =
      s match {
        case (option, dir :: Nil) :: Nil =>
          Right(Some(Path(dir)))
        case Nil => Right(None) // no person found
        case _ => Left("provide filename") // error when parsing
      }

    val tag = scala.reflect.runtime.universe.typeTag[Path] // some magic to make typing work
    val argType = org.rogach.scallop.ArgType.SINGLE
  }

  implicit object DirectoryConverter extends ValueConverter[Directory] {
    // parse is a method, that takes a list of arguments to all option invokations:
    // for example, "-a 1 2 -a 3 4 5" would produce List(List(1,2),List(3,4,5)).
    // parse returns Left with error message, if there was an error while parsing
    // if no option was found, it returns Right(None)
    // and if option was found, it returns Right(...)
    def parse(s:List[(String, List[String])]):Either[String,Option[Directory]] =
      s match {
        case (option, dir :: Nil) :: Nil =>
          Right(Some(Directory(dir)))
        case Nil => Right(None) // no person found
        case _ => Left("provide filename") // error when parsing
      }

    val tag = scala.reflect.runtime.universe.typeTag[Directory] // some magic to make typing work
    val argType = org.rogach.scallop.ArgType.SINGLE
  }
}
