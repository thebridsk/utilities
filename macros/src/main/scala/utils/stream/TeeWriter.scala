package utils.stream

import java.io.Writer

/**
  * @constructor
  * @param writers the streams
  */
class TeeWriter(writers: Writer*) extends Writer {

  override def flush(): Unit = writers.foreach(_.flush())

  override def close(): Unit = writers.foreach(_.close())

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit =
    writers.foreach(_.write(cbuf, off, len))

  override def write(c: Int): Unit = writers.foreach(_.write(c))

  override def write(cbuf: Array[Char]): Unit = writers.foreach(_.write(cbuf))

  override def write(str: String): Unit = writers.foreach(_.write(str))

  override def write(str: String, off: Int, len: Int): Unit =
    writers.foreach(_.write(str, off, len))

  override def append(csq: CharSequence) = {
    writers.foreach(_.append(csq)); this
  }

  override def append(csq: CharSequence, off: Int, len: Int) = {
    writers.foreach(_.append(csq, off, len)); this
  }

  override def append(c: Char) = { writers.foreach(_.append(c)); this }

  /**
    * Get specified writer, and optionally close other writers.
    * This TeeWriter should not be used after calling this method with a true for closeOthers.
    * @param i the index of the writer to return
    * @param closeOthers true if the other writers in this TeeWriter should be closed.
    * @return the writer, null if index is out of range
    * @throws IOException if an error in underlying writer.
    */
  def getWriter(i: Int, closeOthers: Boolean) = {
    var ret: Writer = null;
    if (closeOthers) {
      for (j <- 0 until writers.length) {
        if (i == j) {
          ret = writers(j);
        } else {
          writers(j).close();
        }
      }
    } else {
      if (i < writers.length) {
        ret = writers(i);
      }
    }
    ret;
  }

}
