package esw.gateway.server

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.models.Level.FATAL
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.gateway.api.clients.LoggingClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.ocs.testkit.EswTestKit
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable

class TestAppender(callback: Any => Unit) extends LogAppenderBuilder {
  def apply(system: ActorSystem[_], stdHeaders: JsObject): StdOutAppender =
    new StdOutAppender(system, stdHeaders, callback)
}

class LoggingGatewayTest extends EswTestKit with GatewayCodecs {

  private val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  private val testAppender = new TestAppender(x => {
    logBuffer += Json.parse(x.toString).as[JsObject]
  })

  override def beforeAll(): Unit = {
    super.beforeAll()
    val actorRuntime  = spawnGateway().actorRuntime
    val loggingSystem = actorRuntime.startLogging("logging-gateway-test", "0.0.1")
    loggingSystem.setAppenders(List(testAppender))
  }

  "LoggingApi" must {
    "generate log statement with given app prefix, severity level and message | ESW-200, CSW-63, CSW-78, ESW-279" in {
      val loggingClient = new LoggingClient(gatewayPostClient)

      val componentName = "test_app"
      val prefix        = Prefix(ESW, componentName)
      loggingClient.log(prefix, FATAL, "test-message").futureValue should ===(Done)

      eventually {
        val log: JsObject = logBuffer.filter(_.getString("message").contains("test-message")).head
        log.getString("@componentName") shouldBe componentName
        log.getString("@subsystem") shouldBe ESW.name
        log.getString("@prefix") shouldBe prefix.toString
        log.getString("@severity") shouldBe "FATAL"
      }
    }
  }
}
