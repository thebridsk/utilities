package com.github.thebridsk.utilities.stream

import java.io.OutputStream

class TeeOutputStream(streams: OutputStream*) extends OutputStream {

  override def close(): Unit = streams.foreach(_.close())

  override def flush(): Unit = streams.foreach(_.flush())

  override def write(b: Array[Byte], off: Int, len: Int): Unit =
    streams.foreach(_.write(b, off, len))

  override def write(b: Array[Byte]): Unit = streams.foreach(_.write(b))

  override def write(b: Int): Unit = streams.foreach(_.write(b))

  /**
    * Get specified stream, and optionally close other stream.
    * This TeeOutputStream should not be used after calling this method with a true for closeOthers.
    * @param i the index of the stream to return
    * @param closeOthers
    * @return the stream, null if index is out of range
    * @throws IOException
    */
  def getStream(i: Int, closeOthers: Boolean): OutputStream = {
    var ret: OutputStream = null;
    if (closeOthers) {
      for (j <- 0 to streams.length) {
        if (i == j) {
          ret = streams(j);
        } else {
          streams(j).close();
        }
      }
    } else {
      if (i < streams.length) {
        ret = streams(i);
      }
    }
    ret;
  }

}
