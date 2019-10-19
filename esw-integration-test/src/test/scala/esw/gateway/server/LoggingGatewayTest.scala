package esw.gateway.server

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.models.Level.FATAL
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.gateway.api.clients.LoggingClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.http.core.FutureEitherExt
import mscoket.impl.post.HttpPostTransport
import org.scalatest.WordSpecLike
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TestAppender(callback: Any => Unit) extends LogAppenderBuilder {

  /**
   * A constructor for the TestAppender class.
   *
   * @param system    typed Actor System.
   * @param stdHeaders the headers that are fixes for this service.
   * @return the stdout appender.
   */
  def apply(system: ActorSystem[_], stdHeaders: JsObject): StdOutAppender =
    new StdOutAppender(system, stdHeaders, callback)
}

class LoggingGatewayTest extends ScalaTestFrameworkTestKit with WordSpecLike with FutureEitherExt with GatewayCodecs {
  import frameworkTestKit._

  implicit val typedSystem: ActorSystem[SpawnProtocol] = actorSystem
  implicit val timeout: FiniteDuration                 = 10.seconds

  private val port: Int                           = 6490
  private val gatewayWiring: GatewayWiring        = new GatewayWiring(Some(port))
  private val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  private val testAppender = new TestAppender(x => {
    logBuffer += Json.parse(x.toString).as[JsObject]
  })

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  var loggingSystem: LoggingSystem = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    import gatewayWiring._
    import gatewayWiring.wiring.cswWiring.actorRuntime
    loggingSystem = actorRuntime.startLogging("test", "0.0.1")
    loggingSystem.setAppenders(List(testAppender))
    httpService.registeredLazyBinding
  }

  override protected def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "LoggingApi" must {
    "should generate log statement with given app name, severity level and message | ESW-200" in {
      val postClient    = new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", None)
      val loggingClient = new LoggingClient(postClient)

      val componentName = "test-app"
      loggingClient.log(componentName, FATAL, "test-message").futureValue should ===(Done)

      eventually(logBuffer.size shouldBe 1)
      val log: JsObject = logBuffer.head
      log.getString("@componentName") shouldBe componentName
      log.getString("@severity") shouldBe "FATAL"
      log.getString("message") shouldBe "test-message"
    }
  }
}
