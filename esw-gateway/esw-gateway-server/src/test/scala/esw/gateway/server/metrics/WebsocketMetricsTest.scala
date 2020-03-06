package esw.gateway.server.metrics

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.util.Timeout
import com.lonelyplanet.prometheus.PrometheusResponseTimeRecorder
import csw.command.api.messages.CommandServiceWebsocketMessage.QueryFinal
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Assembly, Sequencer}
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

  private val queryFinalLabelNames        = labels(msg = "ComponentCommand", commandMsg = "QueryFinal")
  private def commandGaugeValue: Double   = getGaugeValue(queryFinalLabelNames)
  private def commandCounterValue: Double = getCounterValue(queryFinalLabelNames)

  private val seqQueryFinalLabelNames       = labels(msg = "SequencerCommand", sequencerMsg = "QueryFinal")
  private def sequencerGaugeValue: Double   = getGaugeValue(seqQueryFinalLabelNames)
  private def sequencerCounterValue: Double = getCounterValue(seqQueryFinalLabelNames)

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
      getGaugeValue shouldBe 0
    }
  }

  "increment websocket gauge on every Command QueryFinal request and decrement it on completion | ESW-197" in {
    when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))

    val queryFinal: WebsocketRequest = ComponentCommand(componentId, QueryFinal(runId, timeout))

    runWsGaugeTest(queryFinal, Completed(runId), commandGaugeValue, commandCounterValue, 1) { p =>
      when(commandService.queryFinal(runId)(timeout)).thenReturn(p.future)
    }
  }

  "increment websocket gauge on every Sequencer QueryFinal request and decrement it on completion | ESW-197" in {
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

//  "Subscribe Events" must {
//    "return set of events successfully | ESW-93, ESW-216" in {
//      val wsClient = WSProbe()
//
//      val eventKey                                   = EventKey("tcs.event.key1")
//      val eventSubscriptionRequest: WebsocketRequest = Subscribe(Set(eventKey), None)
//
//      val event1: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key1"))
//      val event2: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key2"))
//
//      val eventSubscription: EventSubscription = new EventSubscription {
//        override def unsubscribe(): Future[Done] = Future.successful(Done)
//
//        override def ready(): Future[Done] = Future.successful(Done)
//      }
//
//      val eventStream = Source(List(event1, event2)).mapMaterializedValue(_ => eventSubscription)
//
//      when(eventSubscriber.subscribe(Set(eventKey))).thenReturn(eventStream)
//
//      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
//        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
//        isWebSocketUpgrade shouldBe true
//        response shouldEqual event1
//        response shouldEqual event2
//      }
//    }
//
//  }
}
