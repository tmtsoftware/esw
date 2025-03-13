import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._

  override def requires: Plugins = ScoverageSbtPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      coverageEnabled          := false, // XXX TODO FIXME
      coverageMinimumStmtTotal := 80,
      coverageFailOnMinimum    := true,
      coverageHighlighting     := true,
      coverageOutputCobertura  := true,
      coverageOutputXML        := true
    )

}
