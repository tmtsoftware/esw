package agent.app.utils

import agent.app.utils.ProcessOutput.ConsoleWriter
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpecLike}

class ProcessOutputTest extends WordSpecLike with Matchers with Eventually {

  private class FakeConsoleWriter extends ConsoleWriter {
    var data: List[(String, Boolean)] = List.empty

    override def write(value: String): Unit    = data = data.appended((value, false))
    override def writeErr(value: String): Unit = data = data.appended((value, true))
  }

  "attachToProcess" must {
    "attach the output of given process with output of current process | ESW-237" in {
      implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "test-system")
      val fakeConsoleWriter                    = new FakeConsoleWriter()
      val processOutput                        = new ProcessOutput(fakeConsoleWriter)
      val processName                          = "testprocess"
      val process                              = runCommand
      processOutput.attachToProcess(process, processName)

      implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(3, Seconds), Span(500, Millis))

      eventually {
        fakeConsoleWriter.data should contain allElementsOf
        Seq(
          (s"[$processName] stdout text", false),
          (s"[$processName] stderr text", true)
        )
      }
    }
  }

  private def runCommand: Process = {
    val filePath = getClass.getResource("/test-executable.sh").getPath
    val args     = List(filePath)
    val processBuilder = new ProcessBuilder(args: _*)
      .redirectInput(ProcessBuilder.Redirect.INHERIT)

    processBuilder.start()
  }
}
