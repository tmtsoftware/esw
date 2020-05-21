import java.io.File
import java.nio.file.Files

import ohnosequences.sbt.GithubRelease.keys.{ghreleaseAssets, ghreleaseRepoName, ghreleaseRepoOrg, githubRelease}
import ohnosequences.sbt.SbtGithubReleasePlugin
import sbt.Keys._
import sbt.io.{IO, Path}
import sbt.{AutoPlugin, Def, Plugins, ProjectReference, ScopeFilter, Task, ThisProject, librarymanagement, _}

import scala.collection.JavaConverters._

object GithubRelease extends AutoPlugin {
  val coverageReportZipKey = taskKey[File]("Creates a distributable zip file containing the coverage report.")
  val testReportsKey       = taskKey[(File, File)]("Creates test reports in html and zip format.")
  val rtmZipKey            = taskKey[File]("Creates a distributable zip file containing Requirement-Test report.")

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(librarymanagement.Configurations.Compile))

  override def requires: Plugins = SbtGithubReleasePlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      ghreleaseRepoOrg := "tmtsoftware",
      ghreleaseRepoName := EswKeys.projectName,
      aggregate in githubRelease := false,
      // this creates scoverage report zip file and required for GithubRelease task, it assumes that scoverage-report is already generated
      // and is available inside target folder (if it is not present, empty zip will be created)
      coverageReportZipKey := coverageReportZipTask.value,
      testReportsKey := testReportsTask.value,
      rtmZipKey := requirementTestMappingZipTask.value
    )

  private def requirementTestMappingZipTask =
    Def.task {
      lazy val rtmZip = new File(target.value / "ghrelease", "requirement-test-mapping.zip")
      // filter out the index.html
      IO.zip(Path.allSubpaths(new File(target.value, "RTM")).filterNot(_._2.endsWith("html")), rtmZip)
      rtmZip
    }

  private def coverageReportZipTask =
    Def.task {
      lazy val coverageReportZip = new File(target.value / "ghrelease", "scoverage-report.zip")
      IO.zip(Path.allSubpaths(new File(crossTarget.value, "scoverage-report")), coverageReportZip)
      coverageReportZip
    }

  private def testReportsTask =
    Def.task {
      val log = sLog.value

      lazy val testReportZip = target.value / "ghrelease" / "test-reports.zip"
      val testReportHtml     = target.value / "ghrelease" / "test-reports.html"
      val xmlFiles =
        target
          .all(aggregateFilter)
          .value
          .flatMap { targetPath =>
            // allSubpaths includes base path which causes issues while merging, hence excluding it here
            Path.allSubpaths(targetPath / "test-reports").filterNot(_._1.getPath.endsWith("target/test-reports"))
          }

      // 1. include all xml files in single zip
      IO.zip(xmlFiles, testReportZip)
      // 2. generate html report from xml files
      IO.withTemporaryDirectory { dir =>
        // copy xml files from all projects to single directory
        xmlFiles.foreach { case (file, fileName) => Files.copy(file.toPath, (dir / fileName).toPath) }

        // 2.1 create single xml file by merging all xml's
        val xmlFilesDir     = dir.getAbsolutePath
        val mergedXmlReport = s"$xmlFilesDir/test-report.xml"
        log.info(s"Merging all xml files from dir: $xmlFilesDir using junit-merge command.")
        junitMergeCmd(xmlFilesDir, mergedXmlReport)

        // 2.2 create html test report from merged xml
        val htmlReportPath = testReportHtml.getAbsolutePath
        log.info(s"Generating HTML report at path: $htmlReportPath using junit-viewer command.")
        junitViewerCmd(mergedXmlReport, htmlReportPath)
      }
      (testReportZip, testReportHtml)
    }

  private def junitMergeCmd(inputPath: String, outputPath: String) = {
    val commandWithArgs = List("junit-merge", "-d", inputPath, "-o", outputPath)
    new ProcessBuilder(commandWithArgs.asJava).inheritIO.start.waitFor
  }

  private def junitViewerCmd(inputPath: String, outputPath: String) = {
    val commandWithArgs = List("junit-viewer", s"--results=$inputPath", s"--save=$outputPath", "--minify=false")
    new ProcessBuilder(commandWithArgs.asJava).inheritIO.start.waitFor
  }

  def githubReleases(projects: Seq[ProjectReference]): Setting[Task[Seq[sbt.File]]] =
    ghreleaseAssets := {
      val (testReportZip, testReportHtml) = testReportsKey.value
      Seq(
        coverageReportZipKey.value,
        rtmZipKey.value,
        testReportZip,
        testReportHtml
      )
    }
}
