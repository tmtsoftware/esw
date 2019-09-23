package esw.test.reporter

import java.io.{File, FileWriter}
import java.nio.file.Files

import esw.test.reporter.Separators.{NEWLINE, PIPE}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object TestRequirementMapper {

  def main(args: Array[String]): Unit = {

    val (testResultsFile, requirementsFile, outputPath) = args.toList match {
      case t :: r :: o :: Nil => (t, r, o)
      case _ =>
        throw new RuntimeException(
          "**** Provide appropriate parameters. **** \n " +
            "Required parameters : <file with test-story mapping> <file with story-requirement mapping> <output file>"
        )
    }

    // read test-story mapping
    val testResults = Files.readAllLines(new File(testResultsFile).toPath)
    val storyResults = testResults.asScala.toList.map { r =>
      val (story, test, status) = r.split(PIPE).toList match {
        case s :: t :: st :: Nil => (s, t, st)
        case _ =>
          throw new RuntimeException(
            s"**** Provided data is not in valid format : $r ****\n" +
              "Test-Story mapping should be in 'story number | test name | test status' format (Pipe '|' separated format)"
          )
      }
      StoryResult(story.strip(), test.strip(), status.strip())
    }

    // read story-requirement mapping
    val requirementsContent = Files.readAllLines(new File(requirementsFile).toPath)
    val requirements = requirementsContent.asScala.toList.map { r =>
      val (story, requirement) = r.split(PIPE).toList match {
        case s :: r :: Nil => (s, r)
        case _ =>
          throw new RuntimeException(
            s"**** Provided data is not in valid format : $r ****\n" +
              "Story-Requirement mapping should be in 'story number | requirement' format (Pipe '|' separated format)"
          )
      }
      Requirement(story.strip(), requirement.strip())
    }

    // map tests to requirements
    val testAndReqMapped = storyResults.map { s =>
      val correspondingReq = requirements.find(_.story == s.story).getOrElse(Requirement(s.story, "None"))
      TestRequirementMapped(s.story, correspondingReq.number, s.test, s.status)
    }

    // write to file
    Files.createDirectories(new File(outputPath).toPath.getParent)
    val writer = new FileWriter(outputPath)
    testAndReqMapped.map(x => x.format(PIPE) + NEWLINE).foreach(writer.write)
    writer.close()
    println(
      s"**** Successfully mapped Test results to Requirements **** : Check ${new File(outputPath).getCanonicalPath} for results"
    )
  }
}
