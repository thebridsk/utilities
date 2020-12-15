package com.github.thebridsk.utilities.main.utils.test.manual

import java.io.File

object TestPath {

  def main(args: Array[String]): Unit = {
    val f = new File("logs/test.log")
    val p = f.getParent
    println("Parent " + p)
  }
}
