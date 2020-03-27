package esw.gateway.server.metrics

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.testkit.scaladsl.TestSource
import akka.util.Timeout
import com.lonelyplanet.prometheus.PrometheusResponseTimeRecorder
import csw.command.api.messages.CommandServiceWebsocketMessage.QueryFinal
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Assembly, Sequencer}
import csw.params.commands.CommandResponse.Completed
import csw.params.core.models.Id
import csw.params.events.{Event, EventKey, EventName, ObserveEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.TCS
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.WebsocketRequest.{ComponentCommand, SequencerCommand, Subscribe}
import esw.gateway.api.protocol._
import esw.gateway.impl.EventImpl
import esw.gateway.server.CswWiringMocks
import esw.gateway.server.handlers.WebsocketHandlerImpl
import esw.http.core.BaseTestSuite
import esw.ocs.api.protocol.SequencerWebsocketRequest
import io.bullet.borer.Decoder
import msocket.api.ContentEncoding.JsonText
import msocket.api.ContentType
import msocket.impl.CborByteString
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

  private def getGaugeValue(values: List[String]): Double =
    registry.getSampleValue("websocket_active_request_total", labelNames.toArray, values.toArray)

  private def getCounterValue(values: List[String]): Double =
    registry.getSampleValue("websocket_total_messages_per_connection", labelNames.toArray, values.toArray)

  private val labelNames = List(
    "msg",
    "hostname",
    "app_name",
    "command_msg",
    "sequencer_msg",
    "subscribed_event_keys",
    "subscribed_pattern",
    "subsystem"
  )

  private def labels(
      msg: String,
      hostname: String = "unknown",
      appName: String = "unknown",
      commandMsg: String = "",
      sequencerMsg: String = "",
      subscribedEventKeys: String = "",
      subscribedPattern: String = "",
      subsystem: String = ""
  ) =
    List(
      msg,
      hostname,
      appName,
      commandMsg,
      sequencerMsg,
      subscribedEventKeys,
      subscribedPattern,
      subsystem
    )

  private def runWsGaugeTest[Res](
      req: WebsocketRequest,
      res: Res,
      getGaugeValue: => Double,
      getCounterValue: => Double,
      expCounterValue: Double
  )(withMock: Promise[Res] => Unit) = {
    val wsClient = WSProbe()
    val p        = Promise[Res]
    withMock(p)

    WS("/websocket-endpoint", wsClient.flow) ~> wsRoute ~> check {
      getGaugeValue shouldBe 0
      getCounterValue shouldBe 0
      wsClient.sendMessage(ContentType.Json.strictMessage(req))
      Try(wsClient.inProbe.requestNext(1.millis))
      eventually(getGaugeValue shouldBe 1)
      p.complete(Try(res))
      wsClient.expectMessage()
      getCounterValue shouldBe expCounterValue
      eventually(getGaugeValue shouldBe 0)
    }
  }

  "increment websocket gauge on every Command QueryFinal request and decrement it on completion | ESW-197" in {
    val queryFinalLabelNames        = labels(msg = "ComponentCommand", commandMsg = "QueryFinal")
    def commandGaugeValue: Double   = getGaugeValue(queryFinalLabelNames)
    def commandCounterValue: Double = getCounterValue(queryFinalLabelNames)

    when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))

    val queryFinal: WebsocketRequest = ComponentCommand(componentId, QueryFinal(runId, timeout))

    runWsGaugeTest(queryFinal, Completed(runId), commandGaugeValue, commandCounterValue, 1) { p =>
      when(commandService.queryFinal(runId)(timeout)).thenReturn(p.future)
    }
  }

  "increment websocket gauge on every Sequencer QueryFinal request and decrement it on completion | ESW-197" in {
    val seqQueryFinalLabelNames       = labels(msg = "SequencerCommand", sequencerMsg = "QueryFinal")
    def sequencerGaugeValue: Double   = getGaugeValue(seqQueryFinalLabelNames)
    def sequencerCounterValue: Double = getCounterValue(seqQueryFinalLabelNames)

    val sequenceId                = Id()
    val componentId               = ComponentId(Prefix("IRIS.filter.wheel"), Sequencer)
    implicit val timeout: Timeout = Timeout(10.seconds)
    val queryFinalResponse        = Completed(sequenceId)

    when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
    when(sequencer.queryFinal(sequenceId)).thenReturn(Future.successful(queryFinalResponse))

    val seqQueryFinal: WebsocketRequest =
      SequencerCommand(componentId, SequencerWebsocketRequest.QueryFinal(runId, timeout))

    runWsGaugeTest(seqQueryFinal, Completed(runId), sequencerGaugeValue, sequencerCounterValue, 1) { p =>
      when(sequencer.queryFinal(runId)(timeout)).thenReturn(p.future)
    }
  }

  "increment websocket gauge on every Subscribe request and counter per message passing through ws, decrement gauge on completion | ESW-197" in {
    val eventKey                                   = EventKey("tcs.event.key")
    val subscribeLabelNames                        = labels(msg = "Subscribe", subscribedEventKeys = WebsocketRequest.createLabel(Set(eventKey)))
    def subscribeGaugeValue: Double                = getGaugeValue(subscribeLabelNames)
    def subscribeCounterValue: Double              = getCounterValue(subscribeLabelNames)
    val wsClient                                   = WSProbe()
    val eventSubscriptionRequest: WebsocketRequest = Subscribe(Set(eventKey), Some(10))

    val (probe, rawStream) = TestSource.probe[Event].preMaterialize()
    val eventStream        = rawStream.mapMaterializedValue(_ => mock[EventSubscription])
    when(eventSubscriber.subscribe(Set(eventKey), 100.millis, RateLimiterMode)).thenReturn(eventStream)

    WS("/websocket-endpoint", wsClient.flow) ~> wsRoute ~> check {
      subscribeGaugeValue shouldBe 0
      wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
      isWebSocketUpgrade shouldBe true

      publishEvents(10)
      subscribeGaugeValue shouldBe 1
      subscribeCounterValue shouldBe 10

      probe.sendComplete()
      wsClient.expectCompletion()
      eventually(subscribeGaugeValue shouldBe 0)
      subscribeCounterValue shouldBe 10
    }

    def publishEvents(count: Int): Unit = {
      val event: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key1"))
      (1 to count).foreach { _ =>
        probe.sendNext(event)
        decodeMessage[Event](wsClient) shouldEqual event
      }
    }
  }

  private def decodeMessage[T](wsClient: WSProbe)(implicit decoder: Decoder[T]): T = {
    wsClient.expectMessage() match {
      case TextMessage.Strict(text)   => JsonText.decode[T](text)
      case BinaryMessage.Strict(data) => CborByteString.decode[T](data)
      case _                          => throw new RuntimeException("The expected message is not Strict")
    }
  }
}
