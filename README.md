# Utilities

[![Build Status](https://travis-ci.org/thebridsk/utilities.svg?branch=master)](https://travis-ci.org/thebridsk/utilities)

## Development Environment

	git clone https://github.com/thebridsk/utilities.git

## Releasing

To release a new version, the current branch must be `master`, the workspace must be clean.  The `release` branch must not exist.

To create the release, execute:

	sbt release


## Prereqs

- Java 1.8
- [Scala 2.12.4](http://www.scala-lang.org/)
- [SBT 1.0.4](http://www.scala-sbt.org/)
- [Eclipse Oxygen](https://eclipse.org/)
- [Scala IDE](http://scala-ide.org/) [Update site](http://download.scala-ide.org/sdk/lithium/e47/scala212/stable/site)

## SBT Global Setup

- In the SBT install, edit the file `conf/sbtconfig.txt` and make the following changes:

  - Change `-Xmx` option to `-Xmx=4096M`.  512m is not enough.
  - Comment out `-XX:MaxPermSize=256m`.  Doesn't exist in Java 1.8 anymore.
    
- If you update SBT, you may need to clean out the `~/.sbt` directory.  Make sure you save `global.sbt`, `plugins.sbt` and any other configuration files.
- Copy the files in `setup/sbt/0.13` to `~/.sbt/0.13`.  This has a `global.sbt`, `plugins.sbt` files with plugins that are nice to have.


## Setup for Eclipse

The following steps are needed to work in eclipse.

- to generate the eclipse .project and .classpath files:

    cd utilities
    sbt "eclipse with-source=true" "reload plugins" "eclipse with-source=true"

- Import all projects into eclipse starting at the utilities directory.

- In the project-utilities add all the jars from the current SBT in `~\.sbt\boot\`.  As of Dec 2016, this was
`~\.sbt\boot\scala-2.10.6\org.scala-sbt\sbt\0.13.13`.
