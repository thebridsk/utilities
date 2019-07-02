//
// Created Apr 29, 2012
//

package utils.logging;

/**
  * @author whs

  * Constructor
  * @param dateFmtDef
  * @param threadLenDef
  * @param loggerNameLenDef
  * @param useFakeDateDef
  * @param useResourceDef
  * @param useMethodNameDef
  * @param addHeaderDef
  * @param useLevelDef
  */
class ConsoleFormatter(
    dateFmtDef: String,
    threadLenDef: Int = -1,
    loggerNameLenDef: Int = 1,
    useFakeDateDef: Boolean = false,
    useResourceDef: Boolean = true,
    useMethodNameDef: Boolean = false,
    addHeaderDef: Boolean = false,
    useLevelDef: Boolean = true
) extends MyFormatter(
      dateFmtDef,
      threadLenDef,
      loggerNameLenDef,
      useFakeDateDef,
      useResourceDef,
      useMethodNameDef,
      addHeaderDef,
      useLevelDef
    ) {
  def this() = this("HH:mm:ss")
}
