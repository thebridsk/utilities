package com.github.thebridsk.utilities.utils.main.test.manual

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.Subcommand
import com.github.thebridsk.utilities.utils.main.test.TrapExit

object TestScallop {
  def main(args: Array[String]): Unit = {
    object Conf extends ScallopConf(Seq( "xxx")) {
      val apples = opt[Boolean]("apples")
      object tree extends Subcommand("tree") {
        val bananas = opt[Boolean]("bananas")
      }
      addSubcommand(tree)

    }

    println("Calling verify")
    val status = TrapExit.trapExit {
      Conf.verify()
      0
    }
    println(s"Returned from verify, status is ${status}")
  }
}
