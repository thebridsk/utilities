package com.github.thebridsk.utilities.logging

import java.util.logging.StreamHandler
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.io.IOException
import java.text.SimpleDateFormat
import com.github.thebridsk.utilities.stream.MeteredOutputStream
import java.util.ArrayList
import java.util.Arrays
import java.util.logging.LogManager
import java.util.logging.Level
import java.util.logging.Filter
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.security.AccessController
import java.security.PrivilegedAction
import java.io.BufferedOutputStream
import java.util.logging.ErrorManager
import java.util.Date
import java.io.FilenameFilter
import java.util.regex.Pattern

/**
  * Simple file logging <tt>Handler</tt>.
  * <p>
  * The <tt>FileHandler</tt> can either write to a specified file, or it can
  * write to a rotating set of files.
  * <p>
  * For a rotating set of files, as each file reaches a given size limit, it is
  * closed, rotated out, and a new file opened. Rotating files out renames them
  * with a date time stamp in the filename.
  * <p>
  * By default buffering is enabled in the IO libraries but each log record is
  * flushed out when it is complete.
  * <p>
  * By default the {@link MyFormatter} class is used for formatting.
  * <p>
  * <b>Configuration:</b> By default each <tt>FileHandler</tt> is initialized
  * using the following <tt>LogManager</tt> configuration properties. If
  * properties are not defined (or have invalid values) then the specified
  * default values are used.
  * <ul>
  * <li>com.github.thebridsk.utilities.logging.FileHandler.level specifies the default level for the
  * <tt>Handler</tt> (defaults to <tt>Level.ALL</tt>).
  * <li>com.github.thebridsk.utilities.logging.FileHandler.filter specifies the name of a <tt>Filter</tt>
  * class to use (defaults to no <tt>Filter</tt>).
  * <li>com.github.thebridsk.utilities.logging.FileHandler.formatter specifies the name of a
  * <tt>Formatter</tt> class to use (defaults to
  * <tt>java.util.logging.XMLFormatter</tt>)
  * <li>com.github.thebridsk.utilities.logging.FileHandler.encoding the name of the character set encoding
  * to use (defaults to the default platform encoding).
  * <li>com.github.thebridsk.utilities.logging.FileHandler.limit specifies an approximate maximum amount to
  * write (in bytes) to any one file. If this is zero, then there is no limit.
  * (Defaults to no limit).
  * <li>com.github.thebridsk.utilities.logging.FileHandler.count specifies how many output files to cycle
  * through (defaults to 1). 0 indicates no limit
  * <li>com.github.thebridsk.utilities.logging.FileHandler.pattern specifies a pattern for generating the
  * output file name. See below for details. (Defaults to "%h/java%u.log").
  * <li>com.github.thebridsk.utilities.logging.FileHandler.append specifies whether the FileHandler should
  * append onto any existing files (defaults to false).
  * </ul>
  * <p>
  * <p>
  * A pattern consists of a string that includes the following special components
  * that will be replaced at runtime:
  * <ul>
  * <li>"/" the local pathname separator
  * <li>"%t" the system temporary directory
  * <li>"%h" the value of the "user.home" system property
  * <li>"%u" a unique number to resolve conflicts
  * <li>"%d" date time stamp (yyyy.MM.dd.HH.mm.ss.SSS)
  * <li>"%%" translates to a single percent sign "%"
  * </ul>
  * <p>
  * Normally the "%u" unique field is set to 0. However, if the
  * <tt>FileHandler</tt> tries to open the filename and finds the file is
  * currently in use by another process it will increment the unique number field
  * and try again. This will be repeated until <tt>FileHandler</tt> finds a file
  * name that is not currently in use. If there is a conflict and no "%u" field
  * has been specified, it will be added at the end of the filename after a dot.
  * (This will be after any automatically added generation number.)
  * <p>
  * Thus if three processes were all trying to log to fred%u.%d.txt then they
  * might end up using fred0.0.txt, fred1.0.txt, fred2.0.txt as the first file in
  * their rotating sequences.
  * <p>
  * Note that the use of unique ids to avoid conflicts is only guaranteed to work
  * reliably when using a local disk file system.
  * <p>
  * @constructor
  * @param pattern
  * @throws IOException
  * @throws SecurityException
  *
  * @author werewolf
  */
class FileHandler(pattern: String = null) extends StreamHandler {

  private val MAX_UNIQUE = 100;

  private val fSDF = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
  private val dateRegex = """\d\d\d\d\.\d\d\.\d\d\.\d\d\.\d\d\.\d\d\.\d\d\d"""

  private var fUnique = -1;

  private var fPattern = "trace.%u.%d.log";
  private var fLimit: Long = 0;
  private var fCount = 1;
  private var fAppend = false;

  private var fCurrentFile: File = null;

  private var fLockFileName: String = null;
  private var fLockStream: FileOutputStream = null;
  private var fMeter: MeteredOutputStream = null;

  private var fHasDate: Boolean = false

  private var fOldFiles: List[File] = List()

  def setLimit(l: Long) = { fLimit = l }
  def setCount(c: Int) = { fCount = c }

  myCheckAccess();
  configure(pattern);
  cleanupExistingFiles();
  openFiles();

  private def myCheckAccess() = {
    val manager: LogManager = LogManager.getLogManager();
    manager.checkAccess();
  }

  /**
    * @param pattern the pattern to use.  if null, then
    * the &lt;classname>.pattern property in the
    * logging.properties file will be used.
    */
  private def configure(pattern: String) = {
    val cname = getClass().getName();

    fPattern = (if (pattern != null) pattern
                else
                  getStringProperty(cname + ".pattern", "%h/trace.%u.%d.log"))
      .replace('\\', '/');
    fLimit = getIntProperty(cname + ".limit", 0);
    if (fLimit < 0) {
      fLimit = 0;
    }
    fCount = getIntProperty(cname + ".count", 1);
    if (fCount <= 0) {
      fCount = 1;
    }
    fAppend = getBooleanProperty(cname + ".append", false);
    setLevel(getLevelProperty(cname + ".level", Level.ALL));
    setFilter(getClassObjectProperty(classOf[Filter], cname + ".filter", null));
    setFormatter(
      getClassObjectProperty(
        classOf[Formatter],
        cname + ".formatter",
        new FileFormatter()
      )
    );
    try {
      setEncoding(getStringProperty(cname + ".encoding", null));
    } catch {
      case ex: Exception =>
        try {
          setEncoding(null);
        } catch {
          case _: Exception =>
          // doing a setEncoding with null should always work.
          // assert false;
        }
    }
  }

  private def getProperty(key: String) = {
    val manager: LogManager = LogManager.getLogManager();
    manager.getProperty(key);
  }

  private def getStringProperty(key: String, default: String) = {
    val value = getProperty(key);
    if (value != null) {
      value;
    } else {
      default
    }
  }

  private def getLevelProperty(key: String, default: Level): Level = {
    val value = getProperty(key);
    if (value != null) {
      try {
        return Level.parse(value);
      } catch {
        case _: IllegalArgumentException =>
        // ignore errors, return default
      }
    }
    return default
  }

  private def getBooleanProperty(key: String, default: Boolean): Boolean = {
    val value = getProperty(key);
    if (value != null && value.length() > 0) {
      if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("1")) {
        return true;
      }
      if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("0")) {
        return false;
      }
    }
    return default;
  }

  private def getIntProperty(key: String, default: Int): Int = {
    def value = getProperty(key);
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch {
        case _: NumberFormatException =>
        // ignore errors, use default
      }
    }
    return default;
  }

  private def getClassObjectProperty[T](
      cls: Class[T],
      key: String,
      default: T
  ): T = {
    def value = getProperty(key);
    if (value != null) {
      try {
        val keyCls =
          getClass().getClassLoader().loadClass(value).asSubclass(cls);
        return keyCls.getDeclaredConstructor().newInstance();
      } catch {
        case _: Exception =>
        // ignore errors, use default
      }
    }
    return default;
  }

  /* (non-Javadoc)
   * @see java.util.logging.StreamHandler#publish(java.util.logging.LogRecord)
   */
  override def publish(record: LogRecord): Unit = synchronized {
    if (!isLoggable(record)) {
      return;
    }
    try {
      super.publish(record);
      flush();
      if (fLimit > 0 && fMeter.getCurrentSize() >= fLimit) {
        // We performed access checks in the constructor to make sure
        // we are only initialized from trusted code.  So we assume
        // it is OK to write the target files, even if we are
        // currently being called from untrusted code.
        // So it is safe to raise privilege here.
        AccessController.doPrivileged(new PrivilegedAction[Object]() {
          override def run(): Object = {
            rotate();
            return null;
          }
        });
      }
    } catch {
      case x: IOException =>
        println("Current log file is " + fCurrentFile + ": " + x)
        throw x
    }
  }

  /* (non-Javadoc)
   * @see java.util.logging.StreamHandler#close()
   */
  override def close(): Unit = synchronized {
    super.close();
    if (fLockStream != null) {
      try {
        fLockStream.close();
      } catch {
        case _: IOException =>
        // ignore any errors
      }
    }
    new File(fLockFileName).delete();
    fLockFileName = null;
    fLockStream = null;
    fMeter = null;
  }

  var lastDate: Date = null;

  private def getDate() = {
    var d = new Date();
    while (lastDate != null && lastDate.getTime == d.getTime) {
      d = new Date();
    }
    lastDate = d;
    fSDF.format(d)
  }

  private def openFiles(): Unit = {
    acquireLock();
    fCurrentFile = new File(getFileName(fPattern, fUnique, getDate()));
    if (fAppend) {
      open(fCurrentFile, true);
    } else {
      rotate();
    }
  }

  /**
    * Opens the file.
    * @param fname
    * @param append
    * @throws IOException
    */
  private def open(fname: File, append: Boolean): Unit = {
    var len: Long = 0;
    if (append) {
      len = fname.length();
    }
    val fout = new FileOutputStream(fname, append);
    val bout = new BufferedOutputStream(fout);
    fMeter = new MeteredOutputStream(bout, len);
    setOutputStream(fMeter);
  }

  /**
    * Rotate the files.
    * This closes the current file, renames it to include the date,
    * then opens a new file.
    */
  def rotate() {
    val oldLevel = getLevel();
    setLevel(Level.OFF);

    super.close();
    if (!fHasDate) {
      rename();
    } else {
      nextFile()
    }

    try {
      open(fCurrentFile, false);
    } catch {
      case ix: IOException =>
        // We don't want to throw an exception here, but we
        // report the exception to any registered ErrorManager.
        reportError(null, ix, ErrorManager.OPEN_FAILURE);
    }
    setLevel(oldLevel);

  }

  private def nextFile() = {
    fCurrentFile = new File(getFileName(fPattern, fUnique, getDate()))
    prune(fCurrentFile)
  }

  private def rename() = {
    val fn = getFileName(fPattern, fUnique, getDate());
    try {
      val newName = new File(fn);
      fCurrentFile.renameTo(newName);
      prune(newName)
    } catch {
      case e: Exception =>
        // We don't want to throw an exception here, but we
        // report the exception to any registered ErrorManager.
        reportError(
          "Unable to rename " + fCurrentFile + " to " + fn,
          e,
          ErrorManager.GENERIC_FAILURE
        );
    }
  }

  private def prune(nextfile: File) = {
//      println( s"Adding $nextfile" )
    fOldFiles = fOldFiles ::: List(nextfile)
    while (fCount > 0 && fOldFiles.length > fCount) {
      val n = fOldFiles.head
      fOldFiles = fOldFiles.tail
//          println( s"Adding $nextfile, deleting $n" )
      n.delete();
    }
  }

  private def cleanupExistingFiles() = {
    // This is called when starting or rotating file.
    // need to check existing files to see if they match the pattern.
    // If they do, need to add them to fOldFiles in cron order, with oldest at index 0.

    val filename = getFileName(fPattern, fUnique, "", false)
    val parent = new File(filename).getParent
    val dir1 = if (parent == null) "." else parent + File.separator;
    val dir = dir1.replace('\\', '/')
    val reg = if (dir.length() > 0 && fPattern.startsWith(dir)) {
      fPattern.substring(dir.length())
    } else {
      fPattern
    }

    val regex = getFileName(reg, fUnique, "", true)

    val pat = Pattern.compile(regex)

    val dirf = new File(dir)
    val files = Option(dirf.list(new FilenameFilter() {
      def accept(dir1: File, name: String) = {
        pat.matcher(name).matches()
      }
    })).getOrElse(Array())

    import scala.collection.JavaConversions._
    val sortedfiles = files.toList.sorted
//      println(s"Found ${sortedfiles.length} log files with pattern ${pat}")
    if (sortedfiles.length > fCount) {
      val del = sortedfiles.length - fCount
      sortedfiles.take(del).foreach { f =>
        val todelete = new File(dirf, f)
//          println( s"  Deleting ${todelete}" )
        todelete.delete
      }
      sortedfiles.drop(del)
    }
    fOldFiles = sortedfiles.map { f =>
      new File(dirf, f)
    }
  }

  /**
    * @param unique
    * @param date
    * @param regex generate regex to search for log files
    */
  private def getFileName(
      pattern: String,
      unique: Int,
      date: String,
      regex: Boolean = false
  ): String = {
    val u = Integer.toString(unique);
    val b = new StringBuilder();

    var foundUnique = false;
    val len = pattern.length();
    var i = 0;
    import scala.util.control.Breaks._
    breakable {
      while (i < len) {
        var c = pattern.charAt(i);
        if (c == '%') {
          i += 1;
          if (i >= len) {
            // ignore an isolated % at end of string
            break;
          }
          c = pattern.charAt(i);
          c match {
            case '%' =>
              b.append(c);
            case 't' => {
              var tmpDir = System.getProperty("java.io.tmpdir");
              if (tmpDir == null) {
                tmpDir = System.getProperty("user.home");
              }
              b.append(tmpDir);
            }
            case 'h' =>
              b.append(System.getProperty("user.home"));
            case 'u' =>
              foundUnique = true;
              if (regex) {
                b.append("\\d*")
              } else {
                b.append(u);
              }
            case 'd' =>
              if (regex) {
                b.append(dateRegex)
              } else {
                fHasDate = true;
                b.append(date);
              }
            case _ =>
              if (regex) {
                if (FileHandler.special.indexOf(c) >= 0) {
                  b.append('\\')
                }
              }
              b.append(c)
          }
        } else {
          if (regex) {
            if (FileHandler.special.indexOf(c) >= 0) {
              b.append('\\')
            }
          }
          b.append(c)
        }
        i += 1;
      }
    }
    if (!foundUnique) {
      if (regex) {
        b.append("""\d*""")
      } else {
        b.append(u);
      }
    }
    return b.toString();
  }

  private def acquireLock(): Unit = {
    for (i <- 1 until MAX_UNIQUE) {
      fUnique = i
      val fn = getFileName(fPattern, i, "_") + ".lck";
      fLockStream = null;
      var fc = try {
        fLockStream = new FileOutputStream(fn);
        fLockFileName = fn;
        Some(fLockStream.getChannel())
      } catch {
        case e: Exception =>
          if (fLockStream != null) {
            try {
              fLockStream.close();
            } catch {
              case e1: IOException =>
              // ignore any errors
            }
          }
          None;
      }
      fc.foreach(chan => {
        var gotLock = false;
        try {
          val lock = chan.tryLock();
          gotLock = (lock != null);
        } catch {
          case _: IOException =>
            // locking not supported
            gotLock = true;
        }
        if (gotLock) {
          return;
        }
        try {
          fLockStream.close();
        } catch {
          case _: IOException =>
          // ignore
        }
      })
    }
    throw new IOException("Unable to acquire lock for pattern " + fPattern);
  }
}

object FileHandler {

  val special = ".*+?[](){}\\"

}
