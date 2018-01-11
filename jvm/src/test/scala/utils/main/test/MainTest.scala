package utils.main.test

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import utils.logging.Logger
import utils.main.Subcommand
import utils.main.Main
import java.util.concurrent.atomic.AtomicInteger
import utils.classpath.ClassPath

trait Counters {

  val counter: AtomicInteger

  var initCount = -1
  var executeCount = -1
  var cleanupCount = -1

}

object ReturnOptions {
  case class Options( initrc: Option[Int] = Some(0), executerc: Option[Int] = Some(0), cleanupreturn: Boolean = true )

  val optionOk = Options()
  val optionInit1 = Options(Some(1))
  val optionInit2 = Options(Some(2))
  val optionInitEx = Options(None)

  val optionExec1 = Options(Some(0), Some(1))
  val optionExec2 = Options(Some(0), Some(2))
  val optionExecEx = Options(Some(0), None )

  val optionCleanEx = Options(Some(0), Some(0), false)
  val optionExec1CleanEx = Options(Some(0), Some(1), false)
  val optionExecExCleanEx = Options(Some(0), None, false)
  val optionInit1CleanEx = Options(Some(1), Some(0), false)
}

import ReturnOptions._

class ExampleSubcommand( name: String,
                         val counter: AtomicInteger,
                         returnOptions: Options = optionOk
                         ) extends Subcommand(name) with Counters {

  override
  def init() = {
    initCount = counter.incrementAndGet()
    returnOptions.initrc.getOrElse( throw new Exception("from init"))
  }

  def executeSubcommand() = {
    executeCount = counter.incrementAndGet()
    returnOptions.executerc.getOrElse( throw new Exception("from executerc"))
  }

  override
  def cleanup() = {
    cleanupCount = counter.incrementAndGet()
    if (!returnOptions.cleanupreturn) throw new Exception("from cleanup")
  }
}

class SimpleMain( returnOptions: Options = optionOk ) extends Main with Counters {

  val counter = new AtomicInteger(0)

  override
  def init() = {
    initCount = counter.incrementAndGet()
    returnOptions.initrc.getOrElse( throw new Exception("from init"))
  }

  def execute() = {
    executeCount = counter.incrementAndGet()
    returnOptions.executerc.getOrElse( throw new Exception("from executerc"))
  }

  override
  def cleanup() = {
    cleanupCount = counter.incrementAndGet()
    if (!returnOptions.cleanupreturn) throw new Exception("from cleanup")
  }
}

class MainTest extends FlatSpec with MustMatchers {

  TestStartLogging.startLogging()

//  println( ClassPath.show("", getClass.getClassLoader) )

  val testlog = Logger[MainTest]

  behavior of "Main class"

  it should "call execute on main class" in {
    val m = new SimpleMain
    m.mainRun(Array()) mustBe 0
    m.initCount mustBe 1
    m.executeCount mustBe 2
    m.cleanupCount mustBe 3
  }

  it should "not call execute on main class when init returns 1" in {
    val m = new SimpleMain(optionInit1)
    m.mainRun(Array()) mustBe 1
    m.initCount mustBe 1
    m.executeCount mustBe -1
    m.cleanupCount mustBe 2
  }

  it should "not call execute on main class when init returns 2" in {
    val m = new SimpleMain(optionInit2)
    m.mainRun(Array()) mustBe 2
    m.initCount mustBe 1
    m.executeCount mustBe -1
    m.cleanupCount mustBe 2
  }

  it should "not call execute on main class when init throws an exception" in {
    val m = new SimpleMain(optionInitEx)
    m.mainRun(Array()) mustBe 98
    m.initCount mustBe 1
    m.executeCount mustBe -1
    m.cleanupCount mustBe 2
  }

  it should "call execute and return 1" in {
    val m = new SimpleMain(optionExec1)
    m.mainRun(Array()) mustBe 1
    m.initCount mustBe 1
    m.executeCount mustBe 2
    m.cleanupCount mustBe 3
  }

  it should "call execute and return 2" in {
    val m = new SimpleMain(optionExec2)
    m.mainRun(Array()) mustBe 2
    m.initCount mustBe 1
    m.executeCount mustBe 2
    m.cleanupCount mustBe 3
  }

  it should "call execute and return 98" in {
    val m = new SimpleMain(optionExecEx)
    m.mainRun(Array()) mustBe 98
    m.initCount mustBe 1
    m.executeCount mustBe 2
    m.cleanupCount mustBe 3
  }

  it should "clean throws exception and call execute and return 98" in {
    val m = new SimpleMain(optionCleanEx)
    m.mainRun(Array()) mustBe 98
    m.initCount mustBe 1
    m.executeCount mustBe 2
    m.cleanupCount mustBe 3
  }

  it should "clean throws exception and call execute and return 1" in {
    val m = new SimpleMain(optionExec1CleanEx)
    m.mainRun(Array()) mustBe 98
    m.initCount mustBe 1
    m.executeCount mustBe 2
    m.cleanupCount mustBe 3
  }

  it should "clean throws exception and init fails and return 1" in {
    val m = new SimpleMain(optionInit1CleanEx)
    m.mainRun(Array()) mustBe 98
    m.initCount mustBe 1
    m.executeCount mustBe -1
    m.cleanupCount mustBe 2
  }

  behavior of "Main class with subcommand"

  def mainWithSubcommands( mainOptions: Options = optionOk, testOptions: Options = optionOk) = {
    val m = new SimpleMain(mainOptions)
    val t = new ExampleSubcommand("test",m.counter,testOptions)
    m.addSubcommand(t)
    (m,t)
  }

  it should "call execute on test subcommand" in {
    val (m,t) = mainWithSubcommands()
    m.mainRun(Array("test")) mustBe 0
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe 3
    t.cleanupCount mustBe 4
    m.cleanupCount mustBe 5
  }

  it should "not call execute on test subcommand when main init returns 1" in {
    val (m,t) = mainWithSubcommands(optionInit1)
    m.mainRun(Array("test")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe -1
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    t.cleanupCount mustBe -1
    m.cleanupCount mustBe 2
  }

  it should "not call execute on test subcommand when main init throws exception" in {
    val (m,t) = mainWithSubcommands(optionInitEx)
    m.mainRun(Array("test")) mustBe 98
    m.initCount mustBe 1
    t.initCount mustBe -1
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    t.cleanupCount mustBe -1
    m.cleanupCount mustBe 2
  }

  it should "not call execute on test subcommand when init returns 1" in {
    val (m,t) = mainWithSubcommands(optionOk,optionInit1)
    m.mainRun(Array("test")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    t.cleanupCount mustBe 3
    m.cleanupCount mustBe 4
  }

  it should "not call execute on test subcommand when init returns 2" in {
    val (m,t) = mainWithSubcommands(optionOk,optionInit2)
    m.mainRun(Array("test")) mustBe 2
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    t.cleanupCount mustBe 3
    m.cleanupCount mustBe 4
  }

  it should "not call execute on test subcommand when init throws exception" in {
    val (m,t) = mainWithSubcommands(optionOk,optionInitEx)
    m.mainRun(Array("test")) mustBe 98
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    t.cleanupCount mustBe 3
    m.cleanupCount mustBe 4
  }

  it should "return 1 if exec returns 1" in {
    val (m,t) = mainWithSubcommands(optionOk,optionExec1)
    m.mainRun(Array("test")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe 3
    t.cleanupCount mustBe 4
    m.cleanupCount mustBe 5
  }

  it should "return 2 if exec returns 2" in {
    val (m,t) = mainWithSubcommands(optionOk,optionExec2)
    m.mainRun(Array("test")) mustBe 2
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe 3
    t.cleanupCount mustBe 4
    m.cleanupCount mustBe 5
  }

  it should "return 98 if exec throws exception" in {
    val (m,t) = mainWithSubcommands(optionOk,optionExecEx)
    m.mainRun(Array("test")) mustBe 98
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe 3
    t.cleanupCount mustBe 4
    m.cleanupCount mustBe 5
  }

  it should "call execute on test subcommand and return 0 even if cleanup throws exception" in {
    val (m,t) = mainWithSubcommands(optionOk,optionCleanEx)
    m.mainRun(Array("test")) mustBe 0
    m.initCount mustBe 1
    t.initCount mustBe 2
    m.executeCount mustBe -1
    t.executeCount mustBe 3
    t.cleanupCount mustBe 4
    m.cleanupCount mustBe 5
  }

  behavior of "Main class with nested subcommands"

  def mainWith2Subcommands( mainOptions: Options = optionOk, testOptions: Options = optionOk, test2Options: Options = optionOk) = {
    val m = new SimpleMain(mainOptions)
    val t = new ExampleSubcommand("test",m.counter,testOptions)
    val t2 = new ExampleSubcommand("again",m.counter,test2Options)
    t.addSubcommand(t2)
    m.addSubcommand(t)
    (m,t,t2)
  }

  it should "call execute on nested again subcommand" in {
    val (m,t,n) = mainWith2Subcommands()
    m.mainRun(Array("test", "again")) mustBe 0
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe 3
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe 4
    n.cleanupCount mustBe 5
    t.cleanupCount mustBe 6
    m.cleanupCount mustBe 7
  }

  it should "not call execute on nested again subcommand if main init fails" in {
    val (m,t,n) = mainWith2Subcommands(optionInit1)
    m.mainRun(Array("test", "again")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe -1
    n.initCount mustBe -1
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe -1
    n.cleanupCount mustBe -1
    t.cleanupCount mustBe -1
    m.cleanupCount mustBe 2
  }

  it should "not call execute on nested again subcommand if test init fails" in {
    val (m,t,n) = mainWith2Subcommands(optionOk,optionInit1)
    m.mainRun(Array("test", "again")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe -1
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe -1
    n.cleanupCount mustBe -1
    t.cleanupCount mustBe 3
    m.cleanupCount mustBe 4
  }

  it should "not call execute on nested again subcommand if again init fails" in {
    val (m,t,n) = mainWith2Subcommands(optionOk,optionOk,optionInit1)
    m.mainRun(Array("test", "again")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe 3
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe -1
    n.cleanupCount mustBe 4
    t.cleanupCount mustBe 5
    m.cleanupCount mustBe 6
  }

  it should "call execute on nested again subcommand if again init fails" in {
    val (m,t,n) = mainWith2Subcommands(optionOk,optionOk,optionExec1)
    m.mainRun(Array("test", "again")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe 3
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe 4
    n.cleanupCount mustBe 5
    t.cleanupCount mustBe 6
    m.cleanupCount mustBe 7
  }

  it should "call execute on nested again subcommand and return 0 if again cleanup throws exception" in {
    val (m,t,n) = mainWith2Subcommands(optionOk,optionOk,optionCleanEx)
    m.mainRun(Array("test", "again")) mustBe 0
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe 3
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe 4
    n.cleanupCount mustBe 5
    t.cleanupCount mustBe 6
    m.cleanupCount mustBe 7
  }

  it should "call execute on nested again subcommand and return 1 if again cleanup throws exception" in {
    val (m,t,n) = mainWith2Subcommands(optionOk,optionOk,optionExec1CleanEx)
    m.mainRun(Array("test", "again")) mustBe 1
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe 3
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe 4
    n.cleanupCount mustBe 5
    t.cleanupCount mustBe 6
    m.cleanupCount mustBe 7
  }

  it should "call execute on nested again subcommand and execute throw exception, return 98 even if again cleanup throws exception" in {
    val (m,t,n) = mainWith2Subcommands(optionOk,optionOk,optionExecExCleanEx)
    m.mainRun(Array("test", "again")) mustBe 98
    m.initCount mustBe 1
    t.initCount mustBe 2
    n.initCount mustBe 3
    m.executeCount mustBe -1
    t.executeCount mustBe -1
    n.executeCount mustBe 4
    n.cleanupCount mustBe 5
    t.cleanupCount mustBe 6
    m.cleanupCount mustBe 7
  }

}
