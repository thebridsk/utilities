package com.github.thebridsk.utilities.logging

/**
  * @constructor
  * @param name
  * @param value
  * @param resourceBundleName
  */
case class Level(val name: String, value: Int, bundle: String, short: String) {

  /**
    * determine if this level would log given a target level.
    * The target level would be the level of a logger, meaning the level it logs.
    * It could also be the level of a handler.
    * @param target
    */
  def isLoggable(target: Level): Boolean = value >= target.value

  override def toString(): String = {
    name
  }
}

/**
  * The level of a trace message.
  * See java.util.logging.Level for a definition of the levels.
  */
object Level {

  val defaultBundle = null

  val OFF: Level = Level("OFF", Integer.MAX_VALUE, defaultBundle, "OFF");
  val SEVERE: Level = Level("SEVERE", 1000, defaultBundle, "E");
  val WARNING: Level = Level("WARNING", 900, defaultBundle, "W");
  val INFO: Level = Level("INFO", 800, defaultBundle, "I");
  val CONFIG: Level = Level("CONFIG", 700, defaultBundle, "C");
  val FINE: Level = Level("FINE", 500, defaultBundle, "1");
  val FINER: Level = Level("FINER", 400, defaultBundle, "2");
  val FINEST: Level = Level("FINEST", 300, defaultBundle, "3");
  val ALL: Level = Level("ALL", Integer.MIN_VALUE, defaultBundle, "ALL");

  /** Trace level for stdout messages */
  val STDOUT: Level = Level("STDOUT", Level.INFO.value + 2, null, "O");

  /** Trace level for stderr messages */
  val STDERR: Level = Level("STDERR", Level.WARNING.value + 2, null, "R");

  val allLevels: List[Level] = OFF :: SEVERE :: WARNING :: INFO :: CONFIG :: FINE :: FINER :: FINEST :: ALL :: STDOUT :: STDERR :: Nil

  def toLevel(s: String): Option[Level] = {
    allLevels.find(l => l.name == s || l.short == s)

  }
}
