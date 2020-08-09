package com.github.thebridsk.utilities.logging

import java.io.OutputStream
import java.util.logging.LogRecord
import java.io.ByteArrayOutputStream

/**
  * Trace handler for console
  * <p>
  * @author werewolf
  */
class InMemoryHandler extends MyStreamHandler {

  /**
    * The stdout stream
    */
  protected var fOut: OutputStream =
    new ByteArrayOutputStream(10 * 1024)
  setOutputStream(fOut);

  override def publish(record: LogRecord): Unit =
    synchronized {
      super.publish(record);
    }

  /* (non-Javadoc)
   * @see java.util.logging.StreamHandler#isLoggable(java.util.logging.LogRecord)
   */
  override def isLoggable(record: LogRecord): Boolean = {
    val l = record.getLevel();
    if (l == TraceLevel.STDERR || l == TraceLevel.STDOUT) {
      // must not display messages that have STDERR or STDOUT trace levels.
      // These will be displayed by writing directly to the original streams.
      return false;
    }
    return super.isLoggable(record);
  }

  def getLog(): String =
    synchronized {
      fOut.toString()
    }

  def clear(): Unit =
    synchronized {
      fOut = new ByteArrayOutputStream(10 * 1024)
      setOutputStream(fOut);
    }
}
