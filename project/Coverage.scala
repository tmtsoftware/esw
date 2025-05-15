import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._

  override def requires: Plugins = ScoverageSbtPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      // XXX TODO FIXME: Coverage broken in Scala3? See https://contributors.scala-lang.org/t/coverage-broken-why-does-nobody-care/6262/44
      // Coverage results for Scala3 show much lower values than for Scala2.
      coverageEnabled          := true,
      coverageMinimumStmtTotal := 80,
//      coverageFailOnMinimum    := true,
      coverageFailOnMinimum    := false,
      coverageHighlighting     := true,
      coverageOutputCobertura  := true,
      coverageOutputXML        := true
    )

}
