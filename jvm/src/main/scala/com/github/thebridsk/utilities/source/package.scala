package com.github.thebridsk

import org.scalactic.source.Position

package object source {

  implicit class SourcePosition(private val ppos: Position) extends AnyVal {
    def line = s"${pos.fileName}:${pos.lineNumber}"

    def lineForFilename = s"${pos.fileName}_${pos.lineNumber}"

    def pos = ppos
  }

  object SourcePosition {
    implicit def here(implicit pos: Position) = new SourcePosition(pos)
  }

}
