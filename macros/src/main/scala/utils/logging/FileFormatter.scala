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
class FileFormatter(
    dateFmtDef: String,
    threadLenDef: Int = -1,
    loggerNameLenDef: Int = 20,
    useFakeDateDef: Boolean = false,
    useResourceDef: Boolean = true,
    useMethodNameDef: Boolean = true,
    addHeaderDef: Boolean = true,
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
  def this() = this("[yyyy-MM-dd HH:mm:ss:SSS zzz]")
}
