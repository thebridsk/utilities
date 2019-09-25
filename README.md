# Utilities

[![Build Status](https://travis-ci.org/thebridsk/utilities.svg?branch=master)](https://travis-ci.org/thebridsk/utilities)
[![GitHub release](https://img.shields.io/github/release/thebridsk/utilities.svg)](https://github.com/thebridsk/utilities/releases/latest)
[![ZenHub](https://img.shields.io/badge/Managed_with-ZenHub-5e60ba.svg)](https://app.zenhub.com/workspace/o/thebridsk/utilities/boards)

## Development Environment

	git clone https://github.com/thebridsk/utilities.git

## Releasing

To release a new version, the current branch must be `master`, the workspace must be clean.  The `release` branch must not exist.

To create the release, execute:

	sbt release


## Prereqs

- Java 1.8
- [Scala 2.12.10](http://www.scala-lang.org/)
- [SBT 1.3.2](http://www.scala-sbt.org/)

Optional:
- [Visual Studio Code](https://code.visualstudio.com/)
- [Scala Metals](https://scalameta.org/metals/)

## SBT Global Setup

- In the SBT install, edit the file `conf/sbtconfig.txt` and make the following changes:

  - Change `-Xmx` option to `-Xmx=4096M`.  512m is not enough.
  - Comment out `-XX:MaxPermSize=256m`.  Doesn't exist in Java 1.8 anymore.

- If you update SBT, you may need to clean out the `~/.sbt` directory.  Make sure you save `global.sbt`, `plugins.sbt` and any other configuration files.
- Copy the files in `setup/sbt/0.13` to `~/.sbt/0.13`.  This has a `global.sbt`, `plugins.sbt` files with plugins that are nice to have.

## Setup for VSCode

Install the scalametals extension.  Add sbt, Scala and Java to the path when starting VSCode.

## Travis CI

### Installing Travis CLI

    sudo apt-get install gcc ruby ruby-dev ruby-ffi
    sudo gem install travis -v 1.8.8 --no-rdoc --no-ri

### Validating .travis.yml

    travis lint .travis.yml

