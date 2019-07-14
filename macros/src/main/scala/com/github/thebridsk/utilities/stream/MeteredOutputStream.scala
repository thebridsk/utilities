package com.github.thebridsk.utilities.stream

import java.io.OutputStream

class MeteredOutputStream(out: OutputStream, initialcount: Long = 0)
    extends OutputStream {
  private var currentsize = initialcount

  override def write(b: Int): Unit = {
    out.write(b);
    currentsize += 1;
  }

  override def write(b: Array[Byte]): Unit = {
    out.write(b);
    currentsize += b.length;
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    out.write(b, off, len);
    currentsize += len;
  }

  override def flush() = out.flush()

  override def close() = out.close()

  /**
    * @return the count
    */
  def getCurrentSize() = {
    currentsize;
  }

}
