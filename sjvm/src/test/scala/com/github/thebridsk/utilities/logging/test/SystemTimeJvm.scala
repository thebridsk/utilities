package com.github.thebridsk.utilities.logging.test

import com.github.thebridsk.utilities.logging.impl.SystemTime
import com.github.thebridsk.utilities.logging.impl.LoggerImplFactory
import java.time.format.DateTimeFormatter
import java.time.ZoneId

object SystemTimeJvm {

  val fmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

  def apply(): Unit = {

    val st = new SystemTime {
      def currentTimeMillis() = {
        System.currentTimeMillis().toDouble
      }

      /**
        * @param time the time in milliseconds since 1/1/1970
        * @return the returned string has the format HH:mm:ss.SSS
        */
      def formatTime(time: Double): String = {
        fmt.format(java.time.Instant.ofEpochMilli(time.toLong))
      }
    }

    LoggerImplFactory.setSystemTimeObject(st)
  }

}
