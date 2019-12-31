package agent.utils

import agent.utils.helpers.{FakeConsoleWriter, ScalaClassRunner, ScalaTestApp}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpecLike}

class ProcessOutputTest extends WordSpecLike with Matchers with Eventually {
  "attachProcess" must {
    "attach the output of given process with output of current process | ESW-237" in {
      implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "test-system")
      val fakeConsoleWriter                    = new FakeConsoleWriter()
      val processOutput                        = new ProcessOutput(fakeConsoleWriter)
      val processBuilder                       = ScalaClassRunner.run[ScalaTestApp.type]()
      val processName                          = "testprocess"
      val process                              = processBuilder.start()
      processOutput.attachProcess(process, processName)

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
}
