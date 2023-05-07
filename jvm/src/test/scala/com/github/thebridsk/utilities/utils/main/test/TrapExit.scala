package com.github.thebridsk.utilities.utils.main.test

import java.security.Permission

object TrapExit {

  case class ExitTrappedException(status: Int) extends RuntimeException

  /**
    * SecurityManager has been deprecated staring in Java 17.
    *
    * This class in only used to trap the System.exit() and to get status call.
    *
    * See https://www.quora.com/What-is-the-best-way-to-prevent-JVM-from-exiting-after-calling-System-exit-in-Java-programming-language
    * And https://openjdk.org/jeps/411
    * and https://bugs.openjdk.org/browse/JDK-8199704
    *
    * @param old
    */
  @annotation.nowarn
  class MySecurityManager(old: SecurityManager) extends SecurityManager {

    override def checkExit(status: Int): Unit = {
      throw new ExitTrappedException(status)
    }

    override def checkPermission(perm: Permission): Unit = {
      perm match {
        case p: RuntimePermission if p.getName() == "setSecurityManager" =>
          // allow to restore old securitymanager
        case _ =>
          if (old != null) old.checkPermission(perm)
      }
    }

    def restoreOld = {
      setSecurityManager(old)
    }
  }

  @annotation.nowarn
  def getSecurityManager(): SecurityManager = {
    val sm = System.getSecurityManager()
    if (sm != null) println(s"SecurityManager is ${sm.getClass()}")
    sm
  }

  @annotation.nowarn
  def setSecurityManager(sm: SecurityManager): Unit = {
    System.setSecurityManager(sm)
  }

  def trapExit(f: => Int): Int = {
    val newsm = new MySecurityManager(getSecurityManager())
    try {
      setSecurityManager(newsm)
      f
    } catch {
      case ExitTrappedException(status) => status
    } finally {
      newsm.restoreOld
    }
  }
}
