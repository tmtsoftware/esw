package esw.test.reporter

import java.io.{File, FileWriter}
import java.nio.file.Files

import esw.test.reporter.Separators.{COMMA, NEWLINE, PIPE}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object TestRequirementMapper {

  def main(args: Array[String]): Unit = {

    // read program parameters
    val (testResultsFile, requirementsFile, outputPath) = args.toList match {
      case t :: r :: o :: Nil => (t, r, o)
      case _ =>
        throw new RuntimeException(
          "**** Provide appropriate parameters. **** \n " +
            "Required parameters : <file with test-story mapping> <file with story-requirement mapping> <output file>"
        )
    }

    // read test-story mapping
    println("[INFO] Reading test-story mapping file - " + new File(testResultsFile).toPath.toAbsolutePath)
    val testResults = Files.readAllLines(new File(testResultsFile).toPath)
    val storyResults = testResults.asScala.toList.map { line =>
      val (story, test, status) = line.split(PIPE).toList match {
        case s :: t :: st :: Nil => (s, t, st)
        case _ =>
          throw new RuntimeException(
            s"**** Provided data is not in valid format : '$line' ****\n" +
              "Test-Story mapping should be in 'story number | test name | test status' format (Pipe '|' separated format)"
          )
      }
      StoryResult(story.strip(), test.strip(), status.strip())
    }

    // read story-requirement mapping
    println("[INFO] Reading story-requirement mapping file - " + new File(requirementsFile).toPath.toAbsolutePath)
    val requirementsContent = Files.readAllLines(new File(requirementsFile).toPath)

    val requirements = requirementsContent.asScala.toList.map { line =>
      val (story, requirement) = line.splitAt(line.indexOf(COMMA)) match {
        case (s, req) if (!s.isEmpty && !req.isEmpty) => (s, req.drop(1)) // drop to remove the first comma
        case _ =>
          throw new RuntimeException(
            s"**** Provided data is not in valid format : '$line' ****\n" +
              s"Story-Requirement mapping should be in 'story number $COMMA requirement' format (Comma ',' separated format)"
          )
      }

      Requirement(story.strip(), requirement.strip().replaceAll("\"", ""))
    }

    // map tests to requirements
    val testAndReqMapped = storyResults.map { storyResult =>
      val correspondingReq = requirements
        .find(_.story == storyResult.story) // find the Requirements of given story
        .map(_.number)            // take out the Requirement number
        .filter(!_.isEmpty)       // remove if Requirement number is empty
        .getOrElse(Requirement.EMPTY)

      TestRequirementMapped(storyResult.story, correspondingReq, storyResult.test, storyResult.status)
    }

    // write to file
    println("[INFO] Writing results to - " + outputPath)
    Files.createDirectories(new File(outputPath).toPath.getParent)
    val writer = new FileWriter(outputPath)
    testAndReqMapped.map(result => result.format(PIPE) + NEWLINE).foreach(writer.write)
    writer.close()
    println(
      s"**** Successfully mapped Test results to Requirements **** : Check ${new File(outputPath).getCanonicalPath} for results"
    )
  }
}
