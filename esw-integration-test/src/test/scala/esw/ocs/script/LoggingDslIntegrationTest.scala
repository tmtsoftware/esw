package esw.ocs.script

import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.gateway.server.TestAppender
import esw.ocs.api.SequencerApi
import esw.ocs.impl.SequencerActorProxy
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable

class LoggingDslIntegrationTest extends EswTestKit(EventServer) {

  private val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  private val testAppender                        = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])
  var loggingSystem: LoggingSystem                = _
  private var ocsRef: ActorRef[SequencerMsg]      = _
  private var ocsSequencer: SequencerApi          = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    ocsRef = spawnSequencerRef(ESW, "MoonNight")
    ocsSequencer = new SequencerActorProxy(ocsRef)
    loggingSystem = LoggingSystemFactory.start("LoggingDslIntegrationTest", "", "", system)
    loggingSystem.setAppenders(List(testAppender))
  }

  "Script" must {
    "be able to log message | ESW-127, ESW-279" in {
      val command  = Setup(Prefix("TCS.test"), CommandName("log-command"), None)
      val sequence = Sequence(command)

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]
      Thread.sleep(500)

      logBuffer.exists { log =>
        log.getString("@componentName") == "MoonNight" &
        log.getString("@prefix") == "ESW.MoonNight" &
        log.getString("@subsystem") == "ESW" &
        log.getString("@severity") == "FATAL" &
        log.getString("class") == "esw.ocs.scripts.examples.testData.TestScript2" &
        log.getString("message") == "log-message"
      } shouldBe true
    }
  }
}
