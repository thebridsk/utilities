package com.github.thebridsk.utilities.logging

import java.util.logging.Level

/**
  * @constructor
  * @param name
  * @param value
  * @param resourceBundleName
  */
class TraceLevel(name: String, value: Int, resourceBundleName: String = null)
    extends Level(name, value, resourceBundleName)

object TraceLevel {

  /** Trace level for stdout messages */
  val STDOUT = new TraceLevel("STDOUT", Level.INFO.intValue() + 2);

  /** Trace level for stderr messages */
  val STDERR = new TraceLevel("STDERR", Level.WARNING.intValue() + 2);

}
