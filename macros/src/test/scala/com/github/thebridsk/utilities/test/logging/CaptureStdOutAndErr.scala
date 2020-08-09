package com.github.thebridsk.utilities.test.logging

import java.io.PrintStream
import java.io.ByteArrayOutputStream
import com.github.thebridsk.utilities.stream.TeeOutputStream

object CaptureStdOutAndErr {

  class Run {

    val oldStdOut: PrintStream = System.out;
    val oldStdErr: PrintStream = System.err;

    private var stdout = new ByteArrayOutputStream
    private var stderr = new ByteArrayOutputStream

    private var psstdout: PrintStream = null
    private var psstderr: PrintStream = null

    private[CaptureStdOutAndErr] def setup() = {
      psstdout = new PrintStream(new TeeOutputStream(oldStdOut, stdout))
      psstderr = new PrintStream(new TeeOutputStream(oldStdErr, stderr))
      System.setOut(psstdout)
      System.setErr(psstderr)
    }

    private[CaptureStdOutAndErr] def cleanup() = {

      System.out.flush
      System.setOut(oldStdOut)
      System.err.flush
      System.setErr(oldStdErr)

      psstdout = null
      psstderr = null
      stdout = null
      stderr = null
    }

    def getStdout(): String = {
      System.out.flush()
      stdout.toString()
    }

    def getStderr(): String = {
      System.err.flush()
      stderr.toString()
    }
  }

  type RunWithCapture = Run => Unit

  def runWithCapture(f: CaptureStdOutAndErr.RunWithCapture): Unit = {

    val run = new CaptureStdOutAndErr.Run()
    try {
      run.setup()
      f(run)
    } finally {
      run.cleanup()
    }
  }

}
