package esw.test.reporter

import java.io.{File, FileWriter}
import java.nio.file.Files

import org.junit.jupiter.api.extension.{AfterAllCallback, AfterTestExecutionCallback, ExtensionContext}

class KtTestReporter extends AfterTestExecutionCallback with AfterAllCallback {
  var results: List[StoryResult] = List.empty
  override def afterTestExecution(context: ExtensionContext): Unit = {
    val testFailed = context.getExecutionException.isPresent
    addResult(context.getDisplayName, if (testFailed) "FAILED" else "PASSED")
  }

  override def afterAll(context: ExtensionContext): Unit = {
    generateReport()
  }

  private def addResult(name: String, testStatus: String): Unit = {
    val i = name.lastIndexOf(Separators.PIPE)

    val (testName, stories) =
      if (i >= 0) name.splitAt(i)
      else (name, s"${Separators.PIPE} None")

    results ++= stories
      .drop(1)                 // Drop the "|"
      .split(Separators.COMMA) // multiple stories
      .map(x =>
        StoryResult(
          x.split(Separators.PARENTHESIS) { 0 }.strip(),
          testName.split(Separators.PARENTHESIS) { 0 }.strip(),
          testStatus
        )
      )
  }

  private val parentPath = "../../target/RTM"
  private val reportFile = "/testStoryMapping.txt"

  private def generateReport(): Unit = {
    Files.createDirectories(new File(parentPath).toPath)
    val file = new FileWriter(parentPath + reportFile, true)

    // write to file
    results.foreach(x => file.append(x.format(Separators.PIPE) + Separators.NEWLINE))
    file.close()
  }

}
