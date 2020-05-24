//
// Created Apr 29, 2012
//

package com.github.thebridsk.utilities.logging


/**
  * A formatter for logging
  *
  * The logger is configurable via custom properties in a logging.properties.
  * The properties are prefixed with the classname of the logger, this might be a derived class.
  * Example: com.github.thebridsk.utilities.logging.MsgFormatter.dateFormat for the dateFormat property.
  *
  * Properties:
  *   dateFormat=<date format string>
  *               A SimpleDateFormat string for the timestamp.  Default <code>yyyy-MM-dd HH:mm:ss:SSS zzz</code>
  *   timezone=<zone>
  *               The timezone to use.  Defaults to local time.
  *   format=<format string>
  *               The format string for the prefix.  The formatting uses java.util.Formatter.
  *               The arguments to the formatter when formatting the strings are:
  *                 1 - timestamp (String)
  *                 2 - thread (String if useThreadName==true, otherwise Long)
  *                 3 - level (String)
  *                 4 - logger name (String)
  *                 5 - classname (String)
  *                 6 - method name (String)
  *                 7 - message (String)
  *                 8 - short classname (String)
  *                 9 - short loggername (String)
  *               default is: <code>%1$s %2$s %3$s %4$s %5$s.%6$s %7$s</code>
  *   fakeDate=<boolean>
  *               Use a fake date.  This is useful when comparing logfiles.  default is false.
  *   fmtMsg=<boolean>
  *               If true, actually format the message.  Default is true.
  *               If false, just print the message code and the arguments.
  *   useResource=<boolean>
  *               If true, use the resource bundle to get format string for message, the default.
  *               If false, will use the hardcoded value for the format string
  *   showKey=<boolean>
  *               If true, the key for the message in the bundle is added for formatted message.
  *               default is false.
  *   addHeader=<boolean>
  *               If true, a header is added to all log files.
  *   useThreadName=<boolean>
  *               If true, use the thread name, the default
  *               otherwise use the thread id
  *
  * @constructor
  * @param defDateFormat
  * @param defTimezone
  * @param defFormat
  * @param defFakeDate
  * @param defFmtMsg
  * @param defUseResource
  * @param defShowKey
  * @param defAddHeader
  * @param defUseThreadName
  */
class FileFormatter(
    defDateFormat: String = "[yyyy-MM-dd HH:mm:ss:SSS zzz]",
    defTimezone: String = MsgFormatterDefaults.defaultTimezone,

    // 1 - timestamp (String)
    // 2 - thread (String if useThreadName==true, otherwise Long)
    // 3 - level (String)
    // 4 - logger name (String)
    // 5 - classname (String)
    // 6 - method name (String)
    // 7 - message (String)
    // 8 - short classname (String)
    // 9 - short loggername (String)
    defFormat: String = "%1$s %2$s %9$-20s %3$s %5$s.%6$s %7$s",

    defFakeDate: Boolean = false,
    defFmtMsg: Boolean = true,
    defUseResource: Boolean = true,
    defShowKey: Boolean = false,
    defAddHeader: Boolean = true,
    defUseThreadName: Boolean = true,

) extends MsgFormatter(
    defDateFormat = defDateFormat,
    defTimezone = defTimezone,
    defFormat = defFormat,
    defFakeDate = defFakeDate,
    defFmtMsg = defFmtMsg,
    defUseResource = defUseResource,
    defShowKey = defShowKey,
    defAddHeader = defAddHeader,
    defUseThreadName = defUseThreadName
)
