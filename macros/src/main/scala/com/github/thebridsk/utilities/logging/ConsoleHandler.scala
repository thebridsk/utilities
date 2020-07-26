package com.github.thebridsk.utilities.logging

import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.logging.LogRecord

/**
  * Output stream filter that can optionally eat the close() call.
  * <p>
  * @constructor
  * @param o
  * @param eatclose
  * @param name
  * @author werewolf
  */
class EatCloseOutputStream(
    out: OutputStream,
    name: String = null,
    eatClose: Boolean = false
) extends FilterOutputStream(out) {

  override def close(): Unit = {
    if (!eatClose) {
      super.close();
    }
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    out.write(b, off, len);
  }

  override def write(b: Array[Byte]): Unit = {
    out.write(b, 0, b.length);
  }

  override def write(b: Int): Unit = {
    out.write(b);
  }
}

/**
  * Trace handler for console
  * <p>
  * @author werewolf
  */
class ConsoleHandler extends MyStreamHandler {

  /**
    * The stdout stream
    */
  protected val fOut: OutputStream =
    new EatCloseOutputStream(System.out, "out", true);
  setOutputStream(fOut);

  override def publish(record: LogRecord): Unit = synchronized {
    super.publish(record);
    flush();
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

}
