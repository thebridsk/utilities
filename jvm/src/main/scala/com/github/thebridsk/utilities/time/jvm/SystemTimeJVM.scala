package com.github.thebridsk.utilities.time.jvm

import com.github.thebridsk.utilities.time.SystemTime

object SystemTimeJVM {

  def apply(): Unit =
    SystemTime.setTimekeeper(new SystemTime {
      def currentTimeMillis() = System.currentTimeMillis().toDouble
    })

}
