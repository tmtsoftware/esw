package esw.test.reporter

import java.io.FileWriter

import org.scalatest.Reporter
import org.scalatest.events._

class TestReporter extends Reporter {
  var results: Set[StoryResult] = Set.empty

  override def apply(event: Event): Unit = {
    event match {
      case x: TestSucceeded => addResult(x.testName, "PASSED")
      case x: TestFailed    => addResult(x.testName, "FAILED")
      case x: TestIgnored   => addResult(x.testName, "IGNORED")
      case x: TestPending   => addResult(x.testName, "PENDING")
      case x: TestCanceled  => addResult(x.testName, "CANCELED")
      case x: RunCompleted  => writeCSV()
      case _                =>
      //    case RunStarting(ordinal, testCount, configMap, formatter, location, payload, threadName, timeStamp) =>
    }
  }

  private def addResult(name: String, testStatus: String) = {
    val i = name.lastIndexOf('|')

    val (testName, stories) =
      if (i >= 0) name.splitAt(i)
      else (name, "| ESW-000")

    stories
      .drop(1)    // Drop the "|"
      .split(",") // multiple stories
      .foreach { x =>
        val s = x.strip()

        val testNameResult = TestResult(testName.strip(), testStatus)
        val storyResult    = results.find(_.name == s).getOrElse(StoryResult(s, List.empty))

        results -= storyResult
        results += storyResult.copy(tests = storyResult.tests :+ testNameResult)
      }
  }

  private def writeCSV() = {
    val file = new FileWriter("./target/TestStoryMapping.csv", true)
    results.foreach(x => file.append(x.toCSV))
    file.close()
  }
}
