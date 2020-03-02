//package esw.gateway.server.metrics
//
//import akka.actor.typed.{ActorSystem, SpawnProtocol}
//import akka.http.scaladsl.model.ws.Message
//import akka.http.scaladsl.testkit.WSProbe
//import akka.stream.scaladsl.{Sink, Source}
//import akka.util.Timeout
//import com.lonelyplanet.prometheus.PrometheusResponseTimeRecorder
//import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
//import csw.location.api.models.ComponentId
//import csw.params.commands.CommandResponse.Completed
//import csw.params.core.models.Id
//import csw.params.events.EventKey
//import csw.prefix.models.Subsystem.CSW
//import esw.gateway.api.protocol.WebsocketRequest
//import esw.gateway.api.protocol.WebsocketRequest.{ComponentCommand, SequencerCommand, Subscribe, SubscribeWithPattern}
//import esw.gateway.server.CswWiringMocks
//import esw.gateway.server.handlers.WebsocketHandlerImpl
//import esw.http.core.BaseTestSuite
//import esw.ocs.api.protocol.SequencerWebsocketRequest
//import msocket.api.ContentType
//import org.scalatest.prop.TableDrivenPropertyChecks._
//
//import scala.concurrent.{ExecutionContext, Future}
//import scala.concurrent.duration._
//
//class WebsocketHandlerMetricsTest extends BaseTestSuite {
//  private val cswCtxMocks = new CswWiringMocks()
//
//  import cswCtxMocks._
//  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "ws-metrics")
//
//  private val mockedWsHandler   = mock[WebsocketHandlerImpl]
//  private val msg               = mock[Message]
//  private val stream            = Source.single(msg)
//  private val wsHandlerMetrics  = new WebsocketHandlerMetrics(mockedWsHandler, ContentType.Json)(ExecutionContext.global)
//  private val timeout           = Timeout(5.seconds)
//  private var wsClient: WSProbe = _
//
//  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
//
//  private def getGaugeValue(name: String, labels: Map[String, String]): Double =
//    PrometheusResponseTimeRecorder.DefaultRegistry.getSampleValue(name, labels.keys.toArray, labels.values.toArray)
//
//  private def runGaugeTest(wsRequest: WebsocketRequest, label: String) = {
//    when(mockedWsHandler.handle(wsRequest)).thenReturn(stream)
//    def gaugeValue = getGaugeValue(Metrics.websocketGaugeMetricName, Map("msg" -> label))
//
//    gaugeValue shouldBe 0
//    // start 10 ws streams
//    val responses = (1 to 10).map(_ => wsHandlerMetrics.handle(wsRequest))
//    verify(mockedWsHandler, times(10)).handle(wsRequest)
//    gaugeValue shouldBe 10
//
//    // finish first 5 ws streams
//    responses.take(5).foreach(_.runWith(Sink.head).futureValue shouldBe msg)
//    gaugeValue shouldBe 5
//
//    // finish rest 5 ws streams
//    responses.takeRight(5).foreach(_.runWith(Sink.head).futureValue shouldBe msg)
//    gaugeValue shouldBe 0
//  }
//
//  override def beforeEach(): Unit = {
//    wsClient = WSProbe()
//  }
//
//  "Websocket Metrics" must {
//    val componentId = mock[ComponentId]
//    val id          = mock[Id]
//    val eventKey    = mock[EventKey]
//
//    Table(
//      ("WebsocketRequest", "Labels"),
//      (ComponentCommand(componentId, QueryFinal(id, timeout)), "ComponentCommand_QueryFinal"),
//      (ComponentCommand(componentId, SubscribeCurrentState()), "ComponentCommand_SubscribeCurrentState"),
//      (Subscribe(Set(eventKey)), "Subscribe"),
//      (SubscribeWithPattern(CSW, pattern = "move.*"), "SubscribeWithPattern"),
//      (SequencerCommand(componentId, SequencerWebsocketRequest.QueryFinal(id, timeout)), "SequencerCommand_QueryFinal")
//    ).foreach {
//      case (request, label) =>
//        s"increment websocket gauge on every $label request and decrement it on completion | ESW-197" in {
//          runGaugeTest(request, label)
//        }
//    }
//  }
//  val runId                        = Id("123")
//  val componentType                = Assembly
//  val componentId                  = ComponentId(destination, componentType)
//  val queryFinal: WebsocketRequest = ComponentCommand(componentId, QueryFinal(runId, 100.hours))
//
//  "handle queryFinal command and return SubmitResponse command response with metrics enabled" in {
//
//    when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
//    when(commandService.queryFinal(runId)(100.hours)).thenReturn(Future.successful(Completed(runId)))
//
//    WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
//      wsClient.sendMessage(ContentType.Json.strictMessage(queryFinal))
//      isWebSocketUpgrade shouldBe true
//      val response = decodeMessage[SubmitResponse](wsClient)
//      response shouldEqual Completed(runId)
//    }
//  }
//}
