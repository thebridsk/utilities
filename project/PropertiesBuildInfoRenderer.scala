
import sbtbuildinfo._

case class PropertiesBuildInfoRenderer(options: Seq[BuildInfoOption], pkg: String, obj: String) extends ScalaRenderer {

  override def fileType = BuildInfoType.Resource
  override def extension = "properties"
  val traitNames = options.collect{case BuildInfoOption.Traits(ts @ _*) => ts}.flatten
  val objTraits = if (traitNames.isEmpty) "" else " extends " ++ traitNames.mkString(" with ")

  // It is safe to add `import scala.Predef` even though we need to keep `-Ywarn-unused-import` in mind
  // because we always generate code that has a reference to `String`. If the "base" generated code were to be
  // changed and no longer contain a reference to `String`, we would need to remove `import scala.Predef` and
  // fully qualify every reference. Note it is NOT safe to use `import scala._` because of the possibility of
  // the project using `-Ywarn-unused-import` because we do not always generated references that are part of
  // `scala` such as `scala.Option`.
  def header = List(
    "#",
    s"# This object was generated by sbt-buildinfo.",
    "#",
    ""
  )

  override def renderKeys(buildInfoResults: Seq[BuildInfoResult]) =
    header ++
    buildInfoResults.flatMap(line)

  private def line(result: BuildInfoResult): Seq[String] = {
    import result._

    List(
      s"$identifier = ${quote(value)}"
    )
  }

}
