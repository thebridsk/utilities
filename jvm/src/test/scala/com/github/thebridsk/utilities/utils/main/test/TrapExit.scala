package com.github.thebridsk.utilities.utils.main.test

import java.security.Permission

object TrapExit {

  case class ExitTrappedException(status: Int) extends RuntimeException

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

  def getSecurityManager(): SecurityManager = {
    val sm = System.getSecurityManager()
    if (sm != null) println(s"SecurityManager is ${sm.getClass()}")
    sm
  }

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
