package com.github.thebridsk

import org.scalactic.source.Position

package object source {

  implicit class SourcePosition(private val ppos: Position) extends AnyVal {

    /**
      * @return a string with position with syntax: filename:linenumber
      */
    def line: String = s"${pos.fileName}:${pos.lineNumber}"

    /**
      * @return a string with position with syntax: filename_linenumber
      */
    def lineForFilename: String = s"${pos.fileName}_${pos.lineNumber}"

    def pos = ppos
  }

  object SourcePosition {
    implicit def here(implicit pos: Position) = new SourcePosition(pos)
  }

}
