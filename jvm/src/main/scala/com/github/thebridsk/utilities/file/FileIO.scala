package com.github.thebridsk.utilities.file

import scala.io.Codec
import scala.io.Source

import java.io.Writer
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.io.File
import java.nio.file.Path
import java.nio.file.FileSystems
import java.nio.file.StandardCopyOption
import java.nio.file.NoSuchFileException
import java.io.IOException
import com.github.thebridsk.utilities.logging.Logger
import scala.io.BufferedSource
import scala.util.Using
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.nio.file.DirectoryNotEmptyException

object FileIO {
  val log: Logger = Logger(getClass().getName)

  import scala.language.implicitConversions

  implicit val utf8: Codec = Codec.UTF8

  implicit def getPath(filename: String): Path =
    FileSystems.getDefault.getPath(filename)

  implicit def getPath(filename: File): Path =
    FileSystems.getDefault.getPath(filename.toString())

  implicit def getFile(path: Path): File = path.toFile()

  implicit def getFile(filename: String): File = getPath(filename)

  val newsuffix = ".new"
  val newsuffixForPattern = "\\.new"

  def newfilename(filename: String): String = filename + newsuffix

  def readFile(filename: File): String = {
    log.finest("Reading to file " + filename)
    var source: BufferedSource = null
    try {
      source = Source.fromFile(filename)
      source.mkString
    } catch {
      case e: Throwable =>
//        log.severe("Unable to read file "+filename, e)
        throw e
    } finally if (source != null) source.close()
  }

  private def getWriter(filename: File): Writer = {
    new OutputStreamWriter(new FileOutputStream(filename), utf8.charSet)
  }

  def writeFile(filename: File, data: String): Unit = {
    log.finest("Writing to file " + filename)
    try {
      Using.resource(getWriter(filename)) { out =>
        out.write(data)
        out.flush()
      }
    } catch {
      case e: Throwable =>
        log.severe("Unable to write to file " + filename, e)
        throw e
    }
  }

  def deleteFile(path: String): Unit = deleteFile(getPath(path))

  def deleteFile(path: Path): Unit = {
    log.finest("Deleting file " + path)
    try Files.delete(path)
    catch {
      case e: NoSuchFileException =>
        log.fine(
          "attempting to delete a file, " + path + ", that doesn't exist, ignoring"
        )
      case e: Throwable =>
        log.severe("Unable to delete file " + path, e)
        throw e
    }
  }

  /**
    * Recursivily deletes all files
    * @param directory the base directory
    * @param ext an optional extension of files to delete.  MUST NOT start with ".".
    */
  def deleteDirectory(directory: Path, ext: Option[String]): Unit = {
    val extension = ext.map(e => "." + e)
    if (directory.toFile().exists()) {
      if (directory.toFile().isDirectory()) {
        Files.walkFileTree(
          directory,
          new SimpleFileVisitor[Path]() {
            override def visitFile(
                file: Path,
                attrs: BasicFileAttributes
            ): FileVisitResult = {
              val del = extension
                .map { e =>
                  file.toString().toLowerCase().endsWith(e)
                }
                .getOrElse(true)
              if (del) {
//               println(s"Deleting ${file}")
                Files.delete(file);
              }
              FileVisitResult.CONTINUE;
            }

            override def postVisitDirectory(
                dir: Path,
                exc: IOException
            ): FileVisitResult = {
//             println(s"Deleting ${dir}")
              try {
                Files.delete(dir);
              } catch {
                case x: DirectoryNotEmptyException =>
              }
              FileVisitResult.CONTINUE;
            }
          }
        )
      } else {
        directory.toFile().delete()
      }
    }
  }

  def moveFile(source: Path, dest: Path): Unit = {
    log.finest("Moving file " + source + " to " + dest)
    try Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING)
    catch {
      case e: IOException =>
        log.severe(s"Unable to move file $source to $dest, trying again", e)
        Thread.sleep(1000L)
        try Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING)
        catch {
          case e: IOException =>
            log.severe(s"Unable to move file $source to $dest", e)
            throw e
        }
    }
  }

  def copyFile(source: Path, dest: Path): Unit = {
    log.finest("Copying file " + source + " to " + dest)
    try Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
    catch {
      case e: IOException =>
        log.severe("Unable to copy file " + source + " to " + dest, e)
        throw e
    }
  }

  def deleteFileSafe(path: String): Unit = {
    try deleteFile(path)
    catch {
      case e: IOException =>
        try deleteFile(newfilename(path))
        catch {
          case e1: Throwable =>
            e.addSuppressed(e1)
        }
        throw e
    }
    deleteFile(newfilename(path))
  }

  def safeMoveFile(source: Path, dest: Path): Unit = {
    moveFile(source, dest)
  }

  def readFileSafe(filename: String): String = {
    try {
      val nf = newfilename(filename)
      val s = readFile(nf)
      try safeMoveFile(nf, filename)
      catch {
        case e: IOException =>
          log.warning(
            "Suppressing IOException on moving file " + nf + " to " + filename + ": " + e
          )
      }
      s
    } catch {
      case e @ (_: FileNotFoundException | _: NoSuchFileException) =>
        try readFile(filename)
        catch {
          case e1: IOException =>
            e1.addSuppressed(e)
            throw e1
        }
    }
  }

  def writeFileSafe(filename: String, data: String): Unit = {
    val file = getPath(filename)
    val newfile = getPath(newfilename(filename))
    try writeFile(newfile, data)
    catch {
      case e: IOException =>
        try deleteFile(newfile)
        catch {
          case e1: Throwable =>
            e.addSuppressed(e1)
        }
        throw e
    }
    safeMoveFile(newfile, file)
  }

  def onfiles(dir: File): Iterator[Path] = {
    import scala.jdk.CollectionConverters._
    Files.list(dir).iterator().asScala
  }

  def exists(path: String): Boolean = Files.exists(path)

  def mktree(path: Path): Path = Files.createDirectories(path)
}
