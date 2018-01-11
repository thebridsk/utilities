package utils.logging

import java.util.logging.{ Logger => JLogger, FileHandler =>JFileHandler }
import java.util.regex.Pattern
import java.util.logging.Level
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.logging.LogManager
import scala.collection.mutable.TreeSet
import utils.nls.Messages

object Config {
  val fsLog = JLogger.getLogger(classOf[Config].getName(), Messages.BUNDLE_NAME );

    /**
     * The default logging.properties filename.
     */
    val fsLoggingProperties = "logging.properties";
    val fsFileLoggingProperties = new File(fsLoggingProperties);
    val fsDefaultLoggingProperties = "utils/logging/"+fsLoggingProperties

    private[Config] var fsProgramName: Option[String] = None;
    private[Config] var fsProgramVersion: Option[String] = None;
    private[Config] var fsProgramClassLoader: Option[ClassLoader] = None;

    private var initialized = false

    /**
     * Pattern for a trace spec.
     * <p>
     * Groups:
     * <ol>
     * <li>logger name
     * <li>level
     * </ol>
     */
    val fsTraceSpec = Pattern.compile("([^=]+)=([^:]+):?");

    def isInitialized = initialized

    /**
     * Set the name, version and classloader of the program.
     * This only does something the first time it is called.
     * @param name class name of main program
     * @param version
     * @param classloader
     */
    def setProgramInfo( name: String, version: String, classloader: ClassLoader ): Unit = {
      if (fsProgramName.isEmpty) {
        fsProgramName = Some(name)
        fsProgramVersion = Some(version)
        fsProgramClassLoader = Some(classloader)
      }
    }

    /**
     * Initialize by looking for a logger configuration file.
     *
     * Searches in the following order:
     * <ol>
     * <li>logging.properties file in current directory
     * <li>logging.properties in classpath in same directory as main program
     * <li>logging.properties in classpath at utils/logging/logging.properties
     * </ol>
     * The resource searches use the classloader that loaded the main program.
     * <p>
     * When running with ScalaTest, this method will fail and throw an exception.  To prevent
     * this, use the {#initializeForTest} method first.
     */
    def initialize(): Unit = synchronized {
      if (!initialized) {
        if (fsFileLoggingProperties.isFile()) {
          initializeFromFile(fsFileLoggingProperties)
        } else {
          (findProgramLoggingPropertiesResource() match {
            case Some(is) => Some(is)
            case None =>
              getResource(fsDefaultLoggingProperties) match {
                case Some(is) => Some(is)
                case None => None
              }
          }).foreach( resource => {
            val (is, name) = resource
            try {
              initializeFrom(is,"Resource "+name)
            } finally {
              is.close()
            }
          })
        }
      }
      initialized = true
    }

    /**
     * An initialization when running with ScalaTest.
     * The Logging system can't be initialized properly when running with ScalaTest,
     * the system classloader does not have any of the code that is under test.  It is loaded
     * from a child classloader.  The LogManager.readConfiguration call which {#initialize} calls will
     * not work and cause an exception to be thrown.
     * <p>
     * This will initialize the logging Config, such that future calls to {#initialize} will not perform
     * any initialization code.
     */
    def initializeForTest(): Unit = synchronized { initialized = true }

    def findProgramLoggingPropertiesResource(): Option[(InputStream,String)] = {
      if (fsProgramName.isDefined) {
        val dir = getPackageNameAsResource(fsProgramName.get)
        val resource = dir+fsLoggingProperties
        getResource(resource)
      } else {
        None
      }
    }

    def getResource( resource: String ): Option[(InputStream,String)] = {
      try {
        val is = fsProgramClassLoader.getOrElse(getClass.getClassLoader).getResourceAsStream(resource)
        if (is == null) None
        else Some((is,resource))
      } catch {
        case _: Exception => None
      }
    }

    /**
     * Initialize the logging system from the file.
     */
    def initializeFromFile( file: File ): Unit = synchronized {
      if (!initialized) {
        configureFromFile(file)
      }
      initialized = true
    }

    /**
     * Initialize the logging system from a resource
     * @param resource the resource name
     * @param loader the classloader to use in searching for the resource.
     * If null, then the classloader that loaded the program is used.
     */
    def initializeFromResource( resource: String, loader: ClassLoader = null ): Unit = synchronized {
      if (!initialized) {
        configureFromResource(resource, loader)
      }
      initialized = true
    }

    /**
     * Initialize the logging system from an input stream
     * @param is the input stream
     * @param name the name of the stream, used for logging
     */
    def initializeFrom( is: InputStream, name: String ): Unit = synchronized {
      if (!initialized) {
        configureFrom(is,name)
      }
      initialized = true
    }

    /**
     * Initialize the logging system from the file.
     */
    def configureFromFile( file: File ): Unit = synchronized {
        val is = new FileInputStream(file)
        try {
          configureFrom(is, "File "+file)
        } finally {
          is.close()
        }
    }

    /**
     * Initialize the logging system from a resource
     * @param resource the resource name
     * @param loader the classloader to use in searching for the resource.
     * If null, then the classloader that loaded the program is used.
     */
    def configureFromResource( resource: String, loader: ClassLoader = null ): Unit = synchronized {
        val l = {
          if (loader != null) loader
          else fsProgramClassLoader.getOrElse(getClass.getClassLoader)
        }

        val is = l.getResourceAsStream(resource)
        if (is != null) {
          try {
            configureFrom(is, "Resource "+resource)
          } finally {
            is.close()
          }
        }
    }

    /**
     * Initialize the logging system from an input stream
     * @param is the input stream
     * @param name the name of the stream, used for logging
     */
    def configureFrom( is: InputStream, name: String ): Unit = synchronized {
      try {
        val lm = LogManager.getLogManager();
        lm.readConfiguration(is);
        fsLog.info("Successfully configured logging from "+name)
      } catch {
        case x: Throwable =>
          fsLog.log(Level.WARNING,"Exception trying to load logging configuration from "+name, x)
          throw x
      }
    }

    /**
     * Add a {@link FileHandler} to the root logger using the specified logfilename
     * as the output file from the log handler.
     * @param logfilename
     * @throws SecurityException
     * @throws IOException
     */
    def setLogFile( logfilename: String ) = {
        val h = new FileHandler(logfilename);
        val root = JLogger.getLogger("");
        root.addHandler(h);
    }

    /**
     * Set the level on all console handlers on the root logger
     * @param level
     */
    def setLevelOnConsoleHandler( level: Level ) = {
        val root = JLogger.getLogger("");
        for ( h <- root.getHandlers())
        {
            if (h.isInstanceOf[ConsoleHandler])
            {
                h.asInstanceOf[ConsoleHandler].setLevel(level);
            }
        }
    }

    /**
     * Get the level on all console handlers on the root logger
     * @return the level
     */
    def getLevelOnConsoleHandler(): Level = {
        val root = JLogger.getLogger("");
        root.getHandlers.find( h=> h.isInstanceOf[ConsoleHandler]).map( h => h.getLevel ).getOrElse(null)
    }

    /**
     * @return the program name
     */
    def getProgramName() =
    {
        fsProgramName;
    }

    /**
     * @return the program version
     */
    def getProgramVersion() =
    {
        fsProgramVersion;
    }

    /**
     * @return the classloader for the program
     */
    def getprogramClassLoader() =
    {
        fsProgramClassLoader;
    }

    /**
     * Get the package name as a resource.
     * @param cls
     * @return The package name as a resource.  The nil string is returned
     * for the default package.  For other packages, the string will
     * end in a "/".
     */
    def getPackageNameAsResource( cls: Class[_] ): String =
    {
        val fqname = cls.getName();
        getPackageNameAsResource(fqname);
    }

    /**
     * Get the package name as a resource.
     * @param fqname
     * @return The package name as a resource.  The nil string is returned
     * for the default package.  For other packages, the string will
     * end in a "/".
     */
    def getPackageNameAsResource( fqname: String ): String =
    {
        val classIndex = fqname.lastIndexOf('.');
        if (classIndex == -1)
        {
            ""     // default package
        }
        else
        {
            fqname.substring(0, classIndex + 1).replace('.', '/');
        }
    }

}

/**
 *
 * <p>
 * @author werewolf
 */
class Config() {

  import Config._

  /**
     * Set the trace levels of all loggers according to the tracespec.
     * <p>
     * @param spec The new trace specification.
     * Syntax is:
     * <pre>
     * tracespec  := spec
     *               spec ":" tracespec
     * spec       := loggername "=" type
     * loggername := chars ;; a logger name
     *            := "*"   ;; the root logger
     * type       := "SEVERE"|"WARNING"|"INFO"|"FINE"|"FINER"|"FINEST"|"OFF"|"ALL"
     * </pre>
     * <p>
     * Example:
     * *=WARNING:com.example=INFO:com.example.donttrace=OFF
     */
    def setTraceSpec( spec: String ) = synchronized {
        val saveLevel = fsLog.getLevel();
        try
        {
            fsLog.setLevel(Level.FINE);
            fsLog.log(Level.FINE, "Setting tracespec to "+spec);
        }
        finally
        {
            fsLog.setLevel(saveLevel);
        }
        clearLogging();
        val m = fsTraceSpec.matcher(spec);
        while (m.find())
        {
            var loggername = m.group(1);
            if (loggername.equals("*"))
            {
                loggername = "";
            }
            var ltype = m.group(2);

            val l = Level.parse(ltype.toUpperCase());

            val logger = JLogger.getLogger(loggername);
            if (logger != null)
            {
                logger.setLevel(l);
            }
        }
    }

    /**
     * Get the current trace spec.  The format of the returned string is the
     * same format as the {@link #setTraceSpec(String)} argument.
     * @param sep
     * @return the current trace spec
     */
    def getTraceSpec( sep: String = ":"): String =
    {
        val b = new StringBuilder();
        val lm = LogManager.getLogManager();
        val names = new TreeSet[String]();
        val inames = lm.getLoggerNames();
        while (inames.hasMoreElements())
        {
            names.add(inames.nextElement());
        }
        var appendSep = false;
        for (name <- names)
        {
            val l = lm.getLogger(name);
            if (l != null)
            {
                val level = l.getLevel();
                if (level != null)
                {
                    val n =
                    if (name.length() == 0)
                    {
                        "*";
                    } else {
                      name
                    }
                    if (appendSep)
                    {
                        b.append(sep);
                    }
                    appendSep = true;
                    b.append(n).append("=").append(level);
                }
            }
        }
        return b.toString();
    }

    /**
     * set the logging level on all loggers to null.
     */
    private def clearLogging() =
    {
        val logmanager = LogManager.getLogManager();
        val names = logmanager.getLoggerNames();
        while (names.hasMoreElements())
        {
            val name = names.nextElement();
            val l = logmanager.getLogger(name);
            if (l != null)
            {
                l.setLevel(null);
            }
        }
    }

}
