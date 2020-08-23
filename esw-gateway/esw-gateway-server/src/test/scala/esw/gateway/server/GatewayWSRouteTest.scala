package esw.gateway.server

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.messages.CommandServiceStreamRequest.{QueryFinal, SubscribeCurrentState}
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Assembly, Sequencer}
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventKey, EventName, ObserveEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.TCS
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.GatewayStreamRequest.{ComponentCommand, SequencerCommand, Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol._
import esw.gateway.impl.EventImpl
import esw.gateway.server.handlers.GatewayWebsocketHandler
import esw.ocs.api.protocol.SequencerStreamRequest
import esw.testcommons.BaseTestSuite
import io.bullet.borer.Decoder
import msocket.api.ContentEncoding.JsonText
import msocket.api.{ContentType, Subscription}
import msocket.impl.CborByteString
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.ws.WebsocketExtensions.WebsocketEncoding
import msocket.impl.ws.WebsocketRouteFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class GatewayWSRouteTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with ClientHttpCodecs {

  override def clientContentType: ContentType = ContentType.Json

  implicit val typedSystem: ActorSystem[_] = system.toTyped
  private val cswCtxMocks                  = new CswWiringMocks()
  import cswCtxMocks._

  private var wsClient: WSProbe                        = _
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private val eventApi: EventApi   = new EventImpl(eventService, eventSubscriberUtil)
  private val websocketHandlerImpl = new GatewayWebsocketHandler(resolver, eventApi)
  private val route                = new WebsocketRouteFactory("websocket-endpoint", websocketHandlerImpl).make()
  private val destination          = Prefix(TCS, "test")

  override def beforeEach(): Unit = {
    wsClient = WSProbe()
  }

  "QueryFinal for Component" must {

    "return SubmitResponse for a command | ESW-100, ESW-216" in {
      val runId                            = Id("123")
      val componentType                    = Assembly
      val componentId                      = ComponentId(destination, componentType)
      val queryFinal: GatewayStreamRequest = ComponentCommand(componentId, QueryFinal(runId, 100.hours))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.queryFinal(runId)(100.hours)).thenReturn(Future.successful(Completed(runId)))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(queryFinal))
        isWebSocketUpgrade shouldBe true
        val response = decodeMessage[SubmitResponse](wsClient)
        response shouldEqual Completed(runId)
      }
    }

    "return InvalidComponent for invalid component id | ESW-100, ESW-216" in {
      val runId                            = Id("123")
      val componentType                    = Assembly
      val componentId                      = ComponentId(destination, componentType)
      val queryFinal: GatewayStreamRequest = ComponentCommand(componentId, QueryFinal(runId, 100.hours))

      val errmsg = s"No component is registered with id $componentId "

      when(resolver.commandService(componentId)).thenReturn(Future.failed(InvalidComponent(errmsg)))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(queryFinal))
        isWebSocketUpgrade shouldBe true
        decodeMessage[GatewayException](wsClient) shouldEqual InvalidComponent(errmsg)
      }
    }
  }

  "QueryFinal for Sequencer" must {

    "return SubmitResponse for sequence | ESW-250" in {
      val sequenceId                = Id()
      val componentId               = ComponentId(destination, Sequencer)
      implicit val timeout: Timeout = Timeout(10.seconds)

      val queryFinalRequest: GatewayStreamRequest =
        SequencerCommand(componentId, SequencerStreamRequest.QueryFinal(sequenceId, timeout))
      val queryFinalResponse = Completed(sequenceId)

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.queryFinal(sequenceId)).thenReturn(Future.successful(queryFinalResponse))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(queryFinalRequest))
        isWebSocketUpgrade shouldBe true
        val response = decodeMessage[SubmitResponse](wsClient)
        response shouldEqual queryFinalResponse
      }
    }
  }

  "Subscribe current state" must {
    "returns successfully for given componentId | ESW-223, ESW-216" in {
      val componentType                               = Assembly
      val componentId                                 = ComponentId(destination, componentType)
      val stateNames                                  = Set(StateName("stateName1"), StateName("stateName2"))
      val subscribeCurrentState: GatewayStreamRequest = ComponentCommand(componentId, SubscribeCurrentState(stateNames))
      val currentState1                               = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2                               = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStateSubscription = mock[Subscription]
      val currentStateStream       = Source(List(currentState1, currentState2)).mapMaterializedValue(_ => currentStateSubscription)

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.subscribeCurrentState(stateNames)).thenReturn(currentStateStream)

      def response: CurrentState = decodeMessage[CurrentState](wsClient)

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(subscribeCurrentState))
        isWebSocketUpgrade shouldBe true

        response shouldEqual currentState1
        response shouldEqual currentState2
      }

    }
  }

  "Subscribe Events" must {
    "return set of events successfully | ESW-93, ESW-216" in {
      val tcsEventKeyStr1 = "tcs.event.key1"
      val tcsEventKeyStr2 = "tcs.event.key2"
      val eventKey1       = EventKey(tcsEventKeyStr1)
      val eventKey2       = EventKey(tcsEventKeyStr2)
      val eventKeys       = Set(eventKey1, eventKey2)

      val eventSubscriptionRequest: GatewayStreamRequest = Subscribe(eventKeys, None)

      val event1: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key1"))
      val event2: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key2"))

      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1, event2)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.subscribe(eventKeys)).thenReturn(eventStream)

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        def response: Event = decodeMessage[Event](wsClient)

        response shouldEqual event1
        response shouldEqual event2
      }
    }

    "return set of events when subscribe event is sent with maxFrequency = 10 | ESW-93, ESW-216" in {
      val tcsEventKeyStr1 = "tcs.event.key1"
      val tcsEventKeyStr2 = "tcs.event.key2"
      val eventKey1       = EventKey(tcsEventKeyStr1)
      val eventKey2       = EventKey(tcsEventKeyStr2)
      val eventKeys       = Set(eventKey1, eventKey2)

      val eventSubscriptionRequest: GatewayStreamRequest = Subscribe(eventKeys, Some(10))

      val event1: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key1"))
      val event2: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key2"))

      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1, event2)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.subscribe(eventKeys, 100.millis, RateLimiterMode)).thenReturn(eventStream)

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        def response: Event = decodeMessage[Event](wsClient)

        response shouldEqual event1
        response shouldEqual event2
      }
    }

    "return InvalidMaxFrequency is sent with maxFrequency <= 0 | ESW-93, ESW-216" in {
      val tcsEventKeyStr1 = "tcs.event.key1"
      val tcsEventKeyStr2 = "tcs.event.key2"
      val eventKey1       = EventKey(tcsEventKeyStr1)
      val eventKey2       = EventKey(tcsEventKeyStr2)
      val eventKeys       = Set(eventKey1, eventKey2)

      val eventSubscriptionRequest: GatewayStreamRequest = Subscribe(eventKeys, Some(-1))
      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true
        decodeMessage[GatewayException](wsClient) shouldEqual InvalidMaxFrequency()
      }
    }
  }

  "Subscribe events with pattern" must {
    "return set of events on subscribe events with a given pattern | ESW-93, ESW-216" in {
      val eventSubscriptionRequest: GatewayStreamRequest = SubscribeWithPattern(TCS, None, "*")

      val event1: Event = ObserveEvent(Prefix("tcs.test"), EventName("event.key1"))
      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.pSubscribe(TCS, "*")).thenReturn(eventStream)

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[Event](wsClient)

        response shouldEqual event1

      }
    }

    "return set of events when maxFrequency = 5 | ESW-93, ESW-216" in {
      val eventSubscriptionRequest: GatewayStreamRequest = SubscribeWithPattern(TCS, Some(5), "*")
      val event1: Event                                  = ObserveEvent(Prefix("tcs.test"), EventName("event.key1"))

      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.pSubscribe(TCS, "*")).thenReturn(eventStream)
      when(eventSubscriberUtil.subscriptionModeStage(200.millis, RateLimiterMode))
        .thenReturn(new RateLimiterStub[Event](200.millis))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[Event](wsClient)

        response shouldEqual event1

      }
    }

    "return InvalidMaxFrequency when maxFrequency <= 0 | ESW-93, ESW-216" in {
      val eventSubscriptionRequest: GatewayStreamRequest = SubscribeWithPattern(TCS, Some(-1), "*")
      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true
        decodeMessage[GatewayException](wsClient) shouldEqual InvalidMaxFrequency()
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
