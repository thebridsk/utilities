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
class SimpleConsoleFormatter(dateFmtDef: String,
    threadLenDef: Int = 0,
    loggerNameLenDef: Int = 0,
    useFakeDateDef: Boolean = true,
    useResourceDef: Boolean = true,
    useMethodNameDef: Boolean = false,
    addHeaderDef: Boolean = false,
    useLevelDef: Boolean = false) extends MyFormatter(dateFmtDef, threadLenDef, loggerNameLenDef, useFakeDateDef, useResourceDef, useMethodNameDef, addHeaderDef,useLevelDef)
{
  def this() = this("")
}
