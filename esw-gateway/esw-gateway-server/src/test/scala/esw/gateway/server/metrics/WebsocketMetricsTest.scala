package esw.gateway.server.metrics

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import com.lonelyplanet.prometheus.PrometheusResponseTimeRecorder
import csw.command.api.messages.CommandServiceWebsocketMessage.QueryFinal
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.Completed
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.TCS
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.WebsocketRequest.{ComponentCommand, SequencerCommand}
import esw.gateway.api.protocol._
import esw.gateway.impl.EventImpl
import esw.gateway.server.CswWiringMocks
import esw.gateway.server.handlers.WebsocketHandlerImpl
import esw.http.core.BaseTestSuite
import esw.ocs.api.protocol.SequencerWebsocketRequest
import msocket.api.ContentType
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.ws.WebsocketExtensions.WebsocketEncoding
import msocket.impl.ws.WebsocketRouteFactory

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Future, Promise}
import scala.util.Try

class WebsocketMetricsTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with ClientHttpCodecs {

  private val cswCtxMocks = new CswWiringMocks()
  import cswCtxMocks._

  implicit val typedSystem: ActorSystem[_]             = system.toTyped
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(5.seconds)

  private val eventApi         = new EventImpl(eventService, eventSubscriberUtil)
  private val websocketHandler = new WebsocketHandlerImpl(resolver, eventApi, _)
  private val wsRoute          = new WebsocketRouteFactory("websocket-endpoint", websocketHandler).make(metricsEnabled = true)

  private val runId       = Id("123")
  private val componentId = ComponentId(Prefix(TCS, "test"), Assembly)
  private val timeout     = 10.minutes
  private val registry    = PrometheusResponseTimeRecorder.DefaultRegistry

  override def clientContentType: ContentType = ContentType.Json

  private def getGaugeValue(map: Map[String, String]): Double =
    registry.getSampleValue("websocket_active_request_total", map.keys.toArray, map.values.toArray)

  private def labels(msg: String, commandMsg: String = "", sequencerMsg: String = "") =
    Map("msg" -> msg, "hostname" -> "example.com", "command_msg" -> commandMsg, "sequencer_msg" -> sequencerMsg)

  private def commandGaugeValue: Double =
    getGaugeValue(labels(msg = "ComponentCommand", commandMsg = "QueryFinal"))

  private def sequencerGaugeValue: Double =
    getGaugeValue(labels(msg = "SequencerCommand", sequencerMsg = "QueryFinal"))

  private def runWsGaugeTest[Res](req: WebsocketRequest, res: Res, gaugeValue: => Double)(withMock: Promise[Res] => Unit) = {
    val wsClient = WSProbe()
    val p        = Promise[Res]
    withMock(p)

    WS("/websocket-endpoint", wsClient.flow) ~> wsRoute ~> check {
      gaugeValue shouldBe 0
      wsClient.sendMessage(ContentType.Json.strictMessage(req))
      Try(wsClient.inProbe.requestNext(1.millis))
      eventually(gaugeValue shouldBe 1)
      p.complete(Try(res))
      wsClient.expectMessage()
      gaugeValue shouldBe 0
    }
  }

  "increment websocket gauge on every Command QueryFinal request and decrement it on completion | ESW-197" in {
    when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))

    val queryFinal: WebsocketRequest = ComponentCommand(componentId, QueryFinal(runId, timeout))

    runWsGaugeTest(queryFinal, Completed(runId), commandGaugeValue) { p =>
      when(commandService.queryFinal(runId)(timeout)).thenReturn(p.future)
    }
  }

  "increment websocket gauge on every Sequencer QueryFinal request and decrement it on completion | ESW-197" in {
    val seqQueryFinal: WebsocketRequest =
      SequencerCommand(componentId, SequencerWebsocketRequest.QueryFinal(runId, timeout))

    runWsGaugeTest(seqQueryFinal, Completed(runId), sequencerGaugeValue) { p =>
      when(sequencer.queryFinal(runId)(timeout)).thenReturn(p.future)
    }
  }

}
