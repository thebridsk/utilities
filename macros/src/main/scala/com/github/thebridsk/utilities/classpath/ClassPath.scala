package com.github.thebridsk.utilities.classpath

import scala.annotation.tailrec
import java.net.URLClassLoader
import java.util.StringTokenizer

object ClassPath {

  def show(
      linePrefix: String = "",
      loader: ClassLoader = getClass.getClassLoader
  ) = {
    val b = showClasspath(new StringBuilder, linePrefix, loader)
    b.toString()
  }

  def showProperties( linePrefix: String = "" ) = {
    val b = new StringBuilder
    val boot = sys.props.get("java.class.path").getOrElse( sys.props.getOrElse("sun.boot.class.path", "<unknown>"));
    b.append(linePrefix).append("java.class.path").append(fsCRLF)
    val i = linePrefix+"  "
    for (t <- boot.split(sys.props.getOrElse("path.separator", ";"))) {
      b.append(i).append(t).append(fsCRLF);
    }
    b.append(linePrefix).append("System properties:").append(fsCRLF)
    sys.props.foreach { e =>
      b.append(i)
        .append(e._1)
        .append("=")
        .append(e._2)
        .append(fsCRLF)
    }
    b.toString()
  }

  @tailrec
  private def showClasspath(
      b: StringBuilder,
      indent: String,
      loader: ClassLoader
  ): StringBuilder = {
    b.append(indent).append(loader.getClass().getName()).append(fsCRLF);
    val i = indent + "  "
    loader match {
      case urlclassloader: URLClassLoader =>
        for (url <- urlclassloader.getURLs) {
          b.append(i).append(url).append(fsCRLF);
        }
      case _ =>
      // Unknown loader
    }
    val parent = loader.getParent
    if (parent != null) {
      showClasspath(b, indent, parent)
    } else {
      b.append(indent).append("BootClassLoader").append(fsCRLF);
      b
    }
  }

  private val fsCRLF = sys.props.getOrElse("line.separator", "\n");

  def main(args: Array[String]): Unit = {
    println(show())
  }
}
