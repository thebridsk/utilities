package com.github.thebridsk.utilities.logging

import java.util.logging.LogRecord
import java.util.logging.Level

/**
  * Trace handler for the console that writes SEVERE and WARNING messages to stderr.
  * All other levels are written to stdout.
  * <p>
  * @author werewolf
  */
class ConsoleHandlerToErrAndOut extends ConsoleHandler {

  /** */
  protected val fErr = new EatCloseOutputStream(System.err, "err", true);

  private var fUsingStdErr = false;

  override def publish(record: LogRecord) = synchronized {
    val l = record.getLevel();
    val toerr = Level.SEVERE.equals(l) || Level.WARNING.equals(l);

    if (toerr != fUsingStdErr) {
      setOutputStream(if (toerr) fErr else fOut);
      fUsingStdErr = toerr;
    }

    super.publish(record);
  }

}
