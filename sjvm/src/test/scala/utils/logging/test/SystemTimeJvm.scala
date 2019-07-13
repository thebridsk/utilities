package com.github.thebridsk.utilities.logging.test

import com.github.thebridsk.utilities.logging.impl.SystemTime
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object SystemTimeJvm {

  def apply() = {

    val st = new SystemTime {
      def currentTimeMillis() = {
        System.currentTimeMillis()
      }

      val fmt = new SimpleDateFormat("HH:mm:ss.SSS")

      /**
       * @param time the time in milliseconds since 1/1/1970
       * @return the returned string has the format HH:mm:ss.SSS
       */
      def formatTime( time: Double ): String = {
        fmt.format(new Date(time.toLong))
      }
    }

    LoggerImplFactory.setSystemTimeObject(st)
  }

}
