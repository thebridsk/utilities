package com.github.thebridsk.utilities.logging.test

import com.github.thebridsk.utilities.logging.impl.SystemTime
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory
import scala.scalajs.js.Date

object SystemTimeJs {

  def apply(): Unit = {

    val st = new SystemTime {
      def currentTimeMillis() = {
        val d = new Date
        d.getTime()
      }

      /**
        * @param time the time in milliseconds since 1/1/1970
        * @return the returned string has the format HH:mm:ss.SSS
        */
      def formatTime(time: Double): String = {
        formatLogTime(time)
      }

      final def formatLogTime(time: Double): String = {
        val d = new Date(time)

        if (true) {
          val hour = d.getHours().toInt
          val min = d.getMinutes().toInt
          val sec = d.getSeconds().toInt
          val milli = d.getMilliseconds().toInt
          f"$hour%d:$min%02d:$sec%02d.$milli%03d"
        } else {

          d.toLocaleDateString() + " " + d.toLocaleTimeString()
        }
      }
    }

    LoggerImplFactory.setSystemTimeObject(st)
  }

}
