package esw.gateway.server.metrics

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import csw.location.api.models.ComponentId
import csw.params.core.models.Id
import csw.params.events.EventKey
import csw.prefix.models.Subsystem.CSW
import esw.gateway.api.protocol.WebsocketRequest
import esw.gateway.api.protocol.WebsocketRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.server.handlers.WebsocketHandlerImpl
import esw.gateway.server.metrics.CommandMetrics._
import esw.gateway.server.metrics.EventMetrics._
import esw.http.core.BaseTestSuite
import msocket.api.ContentType
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class WebsocketHandlerMetricsTest extends BaseTestSuite {
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "ws-metrics")

  private val mockedWsHandler  = mock[WebsocketHandlerImpl]
  private val msg              = mock[Message]
  private val stream           = Source.single(msg)
  private val wsHandlerMetrics = new WebsocketHandlerMetrics(mockedWsHandler, ContentType.Json)(ExecutionContext.global)
  private val timeout          = Timeout(5.seconds)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private def getGaugeValue(name: String, labels: Map[String, String]): Double =
    Metrics.prometheusRegistry.getSampleValue(name, labels.keys.toArray, labels.values.toArray)

  private def runGaugeTest(wsRequest: WebsocketRequest, metricName: String, labels: Map[String, String]) = {
    when(mockedWsHandler.handle(wsRequest)).thenReturn(stream)
    def gaugeValue = getGaugeValue(metricName, labels)

    gaugeValue shouldBe 0
    // start 10 ws streams
    val responses = (1 to 10).map(_ => wsHandlerMetrics.handle(wsRequest))
    verify(mockedWsHandler, times(10)).handle(wsRequest)
    gaugeValue shouldBe 10

    // finish first 5 ws streams
    responses.take(5).foreach(_.runWith(Sink.head).futureValue shouldBe msg)
    gaugeValue shouldBe 5

    // finish rest 5 ws streams
    responses.takeRight(5).foreach(_.runWith(Sink.head).futureValue shouldBe msg)
    gaugeValue shouldBe 0
  }

  "Command Service Metrics" must {
    val componentId = mock[ComponentId]
    val id          = mock[Id]

    Table(
      ("CommandServiceWebsocketMessage", "MetricName", "Labels"),
      (QueryFinal(id, timeout), queryFinalGaugeMetricName, Map("api"   -> "query_final")),
      (SubscribeCurrentState(), currentStateGaugeMetricName, Map("api" -> "subscribe_current_state"))
    ).foreach {
      case (msg, metricName, labels) =>
        val apiName = msg.getClass.getSimpleName
        s"increment $apiName gauge on every $apiName request and decrement it on completion | ESW-197" in {
          val wsRequest = WebsocketRequest.ComponentCommand(componentId, msg)
          runGaugeTest(wsRequest, metricName, labels)
        }
    }
  }

  "Event Service Metrics" must {
    val eventKey = mock[EventKey]

    Table(
      ("WebsocketRequest", "MetricName", "Labels"),
      (Subscribe(Set(eventKey)), subscribeGaugeMetricName, Map("api" -> "subscribe_event")),
      (
        SubscribeWithPattern(CSW, pattern = "move.*"),
        patternSubscribeGaugeMetricName,
        Map("api" -> "pattern_subscribe_event", "subsystem" -> "CSW", "pattern" -> "move.*")
      )
    ).foreach {
      case (wsRequest, metricName, labels) =>
        val apiName = wsRequest.getClass.getSimpleName
        s"increment $apiName gauge on every $apiName request and decrement it on completion | ESW-197" in {
          runGaugeTest(wsRequest, metricName, labels)
        }
    }
  }
}
