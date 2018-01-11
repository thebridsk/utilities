package utils.logging

/**
 * @constructor
 * @param name
 * @param value
 * @param resourceBundleName
 */
case class Level( val name: String, value: Int, bundle: String, short: String ) {
  /**
   * determine if this level would log given a target level.
   * The target level would be the level of a logger, meaning the level it logs.
   * It could also be the level of a handler.
   * @param target
   */
  def isLoggable( target: Level ) =  value >= target.value

  override
  def toString() = {
    name
  }
}

/**
 * The level of a trace message.
 * See java.util.logging.Level for a definition of the levels.
 */
object Level {

  val defaultBundle = null

  val OFF = Level("OFF",Integer.MAX_VALUE, defaultBundle,"OFF");
  val SEVERE = Level("SEVERE",1000, defaultBundle,"E");
  val WARNING = Level("WARNING", 900, defaultBundle,"W");
  val INFO = Level("INFO", 800, defaultBundle,"I");
  val CONFIG = Level("CONFIG", 700, defaultBundle,"C");
  val FINE = Level("FINE", 500, defaultBundle,"1");
  val FINER = Level("FINER", 400, defaultBundle,"2");
  val FINEST = Level("FINEST", 300, defaultBundle,"3");
  val ALL = Level("ALL", Integer.MIN_VALUE, defaultBundle,"ALL");

  /** Trace level for stdout messages */
  val STDOUT = Level("STDOUT", Level.INFO.value+2,null,"O");
  /** Trace level for stderr messages */
  val STDERR = Level("STDERR", Level.WARNING.value+2,null,"R");

  val allLevels = OFF::SEVERE::WARNING::INFO::CONFIG::FINE::FINER::FINEST::ALL::STDOUT::STDERR::Nil

  def toLevel( s: String ) = {
    allLevels.find( l => l.name==s || l.short == s )

  }
}
