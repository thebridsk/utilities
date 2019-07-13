package com.github.thebridsk.utilities.logging

import java.util.logging.{Logger => JLogger}
import java.io.PrintStream
import java.util.logging.Level
import com.github.thebridsk.utilities.stream.TeeOutputStream

object RedirectOutput {
  private val fsLog =
    JLogger.getLogger(getClass.getName(), null /* resource bundle */ )

  private var fsOldStdOut: PrintStream = null;
  private var fsOldStdErr: PrintStream = null;

  private var fsTraceOut: TraceOutputStream = null;
  private var fsTraceErr: TraceOutputStream = null;

  private def getLevel(l: Level, default: Level) = if (l == null) default else l

  /**
    * Capture and trace standard out and standard error calls.
    * <p>
    * The logger name that is used for standard out is "com.github.thebridsk.utilities.logging.RedirectOutput.StdOut",
    * and "com.github.thebridsk.utilities.logging.RedirectOutput.StdErr" for standard err.
    * @param stdout The trace level to use for stdout.  If null, then {@link TraceLevel#STDOUT} is used.
    * @param stderr The trace level to use for stderr.  If null, then {@link TraceLevel#STDERR} is used.
    */
  def traceStandardOutAndErr(stdout: Level = null, stderr: Level = null) {

    fsOldStdOut = System.out;
    fsOldStdErr = System.err;

    try {
      fsTraceOut = new TraceOutputStream(
        getLevel(stdout, TraceLevel.STDOUT),
        getClass.getName() + ".StdOut"
      );
      System.setOut(
        new PrintStream(new TeeOutputStream(fsOldStdOut, fsTraceOut))
      );
    } catch {
      case e: Exception =>
        if (fsLog.isLoggable(Level.FINE)) {
          fsLog.log(Level.FINE, "Unable to create TraceOutputStream", e);
        }
        fsOldStdOut = null;
    }
    try {
      fsTraceErr = new TraceOutputStream(
        getLevel(stderr, TraceLevel.STDERR),
        getClass.getName() + ".StdErr"
      );
      System.setErr(
        new PrintStream(new TeeOutputStream(fsOldStdErr, fsTraceErr))
      );
    } catch {
      case e: Exception =>
        if (fsLog.isLoggable(Level.FINE)) {
          fsLog.log(Level.FINE, "Unable to create TraceOutputStream", e);
        }
        fsOldStdErr = null;
    }
  }

  /**
    *
    */
  def restoreStandardOutAndErr(): Unit = {
    if (fsOldStdOut != null) {
      System.setOut(fsOldStdOut);
    }
    if (fsOldStdErr != null) {
      System.setErr(fsOldStdErr);
    }
  }

}
