package com.github.thebridsk.utilities.logging

import java.util.logging.StreamHandler

class MyStreamHandler extends StreamHandler {

//  override
//  def setFormatter( newFormatter: Formatter ): Unit = {
//    val cn = newFormatter.getClass.getName
//    val lm = LogManager.getLogManager
//    val want = lm.getProperty(getClass.getName+".formatter")
//    if (want != null && want != cn) {
//      println("Replacing formatter "+cn+" with "+want )
//      val cls = getClass.getClassLoader.loadClass(want)
//      val nf = cls.newInstance().asInstanceOf[Formatter]
//      super.setFormatter(nf)
//    } else {
//      super.setFormatter(newFormatter)
//    }
//  }

}
