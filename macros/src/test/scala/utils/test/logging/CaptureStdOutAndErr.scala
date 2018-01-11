package utils.test.logging

import java.io.PrintStream
import java.io.ByteArrayOutputStream
import utils.stream.TeeOutputStream

object CaptureStdOutAndErr {

    class Run {

      val oldStdOut: PrintStream = System.out;
      val oldStdErr: PrintStream = System.err;

      private var stdout = new ByteArrayOutputStream
      private var stderr = new ByteArrayOutputStream

      private var psstdout: PrintStream = null
      private var psstderr: PrintStream = null

      private[CaptureStdOutAndErr] def setup() = {
        psstdout = new PrintStream( new TeeOutputStream( oldStdOut, stdout) )
        psstderr = new PrintStream( new TeeOutputStream( oldStdErr, stderr) )
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

      def getStdout() = {
        System.out.flush()
        stdout.toString()
      }

      def getStderr() = {
        System.err.flush()
        stderr.toString()
      }
    }

    type RunWithCapture = Run => Unit

    def runWithCapture( f: CaptureStdOutAndErr.RunWithCapture ) = {

      val run = new CaptureStdOutAndErr.Run()
      try {
        run.setup()
        f(run)
      } finally {
        run.cleanup()
      }
    }

}
