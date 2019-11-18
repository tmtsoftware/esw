package esw.ocs.script

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequenceAndWait
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.gateway.server.TestAppender
import esw.ocs.api.BaseTestSuite
import esw.ocs.app.wiring.SequencerWiring
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class LoggingDslIntegrationTest extends ScalaTestFrameworkTestKit with BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  private implicit val timeout: Timeout                        = 10.seconds

  private var ocsWiring: SequencerWiring     = _
  private var ocsRef: ActorRef[SequencerMsg] = _

  private val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  private val testAppender = new TestAppender(x => {
    logBuffer += Json.parse(x.toString).as[JsObject]
  })

  var loggingSystem: LoggingSystem = _

  protected override def beforeAll(): Unit = {
    super.beforeAll()
    ocsWiring = new SequencerWiring("ocs", "moonnight", None)
    ocsWiring.sequencerServer.start()
    loggingSystem = ocsWiring.actorRuntime.startLogging("", "")
    loggingSystem.setAppenders(List(testAppender))
    ocsRef = ocsWiring.sequencerRef
  }

  protected override def afterAll(): Unit = {
    ocsWiring.sequencerServer.shutDown()
    super.afterAll()
  }

  "Script" must {
    "be able to log message | ESW-127" in {
      val command                     = Setup(Prefix("TCS.test"), CommandName("log-command"), None)
      val sequence                    = Sequence(command)
      val res: Future[SubmitResponse] = ocsRef ? (SubmitSequenceAndWait(sequence, _))

      res.futureValue should ===(Completed(sequence.runId))

      Thread.sleep(500)

      val log: JsObject = logBuffer.head
      log.getString("@componentName") shouldBe "ocs@moonnight"
      log.getString("@severity") shouldBe "FATAL"
      log.getString("prefix") shouldBe "Prefix(esw.ocs.prefix5)"
      log.getString("class") shouldBe "esw.ocs.scripts.examples.testData.TestScript2"
      log.getString("message") shouldBe "log-message"
    }
  }

}
