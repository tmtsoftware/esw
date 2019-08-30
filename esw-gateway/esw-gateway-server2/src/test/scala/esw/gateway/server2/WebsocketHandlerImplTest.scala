package esw.gateway.server2

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import csw.command.api.CurrentStateSubscription
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.core.models.Subsystem.TCS
import csw.params.core.models.{Id, Prefix}
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventKey, EventName, ObserveEvent}
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.messages._
import esw.gateway.api.{CommandApi, EventApi}
import esw.gateway.impl.{CommandImpl, EventImpl}
import esw.http.core.BaseTestSuite
import io.bullet.borer.Decoder
import mscoket.impl.ws.Encoding.JsonText
import mscoket.impl.{HttpCodecs, RoutesFactory}
import org.mockito.Mockito.when

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class WebsocketHandlerImplTest extends BaseTestSuite with ScalatestRouteTest with RestlessCodecs with HttpCodecs {
  private val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  private val cswCtxMocks = new CswContextMocks(actorSystem)
  import cswCtxMocks._

  implicit val timeout: Timeout                        = Timeout(5.seconds)
  private var wsClient: WSProbe                        = _
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  private val commandApi: CommandApi = new CommandImpl(componentFactory.commandService)
  private val websocketHandlerImpl   = new WebsocketHandlerImpl(commandApi, eventApi)
  lazy val routesFactory             = new RoutesFactory[PostRequest, WebsocketRequest](null, websocketHandlerImpl, null)
  private val route                  = new Routes(routesFactory.route, handlers).route

  override def beforeEach(): Unit = {
    wsClient = WSProbe()
  }

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  "WebsocketHandlerImpl" must {

    "return SubmitResponse on queryFinal for a command | ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val componentId   = ComponentId(componentName, componentType)
      val queryFinal    = QueryFinal(componentId, runId)

      when(componentFactory.commandService(componentName, componentType)).thenReturn(Future.successful(commandService))
      when(commandService.queryFinal(runId)(100.hours)).thenReturn(Future.successful(Completed(runId)))

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(queryFinal))
        isWebSocketUpgrade shouldBe true
        val response = decodeMessage[Either[InvalidComponent, SubmitResponse]](wsClient)
        response.rightValue shouldEqual Completed(runId)
      }
    }

    "return InvalidComponent for invalid component id on queryFinal for a command  | ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val componentId   = ComponentId(componentName, componentType)
      val queryFinal    = QueryFinal(componentId, runId)

      val errmsg = s"Could not find component $componentName of type - $componentType"

      when(componentFactory.commandService(componentName, componentType))
        .thenReturn(Future.failed(new IllegalArgumentException(errmsg)))

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(queryFinal))
        isWebSocketUpgrade shouldBe true
        val response = decodeMessage[Either[InvalidComponent, SubmitResponse]](wsClient)
        response.leftValue shouldEqual InvalidComponent(errmsg)
      }
    }

    "subscribe current state for given componentId | ESW-216" in {
      val componentName         = "test"
      val componentType         = Assembly
      val componentId           = ComponentId(componentName, componentType)
      val stateNames            = Set(StateName("stateName1"), StateName("stateName2"))
      val subscribeCurrentState = SubscribeCurrentState(componentId, stateNames, None)
      val currentState1         = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2         = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStateSubscription = mock[CurrentStateSubscription]
      val currentStateStream = Source(List(currentState1, currentState2))
        .mapMaterializedValue(_ => currentStateSubscription)

      when(componentFactory.commandService(componentName, componentType)).thenReturn(Future.successful(commandService))
      when(commandService.subscribeCurrentState(stateNames)).thenReturn(currentStateStream)

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(subscribeCurrentState))
        isWebSocketUpgrade shouldBe true

        val responseSet = Source(1 to 2)
          .map(_ => decodeMessage[Either[CommandError, CurrentState]](wsClient))
          .runWith(Sink.seq)
          .futureValue
          .map(_.rightValue)
          .toSet

        responseSet shouldEqual Set(currentState1, currentState2)
      }

    }

    "return InvalidMaxFrequency when subscribe current state is sent with maxFrequency <= 0 | ESW-216" in {
      val componentName         = "test"
      val componentType         = Assembly
      val componentId           = ComponentId(componentName, componentType)
      val stateNames            = Set(StateName("stateName1"), StateName("stateName2"))
      val subscribeCurrentState = SubscribeCurrentState(componentId, stateNames, Some(-1))

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(subscribeCurrentState))
        isWebSocketUpgrade shouldBe true

        val response = Source(1 to 10)
          .map(_ => decodeMessage[Either[CommandError, CurrentState]](wsClient))
          .runWith(Sink.head)
          .leftValue

        response shouldEqual InvalidMaxFrequency()
      }
    }

    "return set of events on subscribe events | ESW-216" in {
      val tcsEventKeyStr1 = "tcs.event.key1"
      val tcsEventKeyStr2 = "tcs.event.key2"
      val eventKey1       = EventKey(tcsEventKeyStr1)
      val eventKey2       = EventKey(tcsEventKeyStr2)
      val eventKeys       = Set(eventKey1, eventKey2)

      val eventSubscriptionRequest = Subscribe(eventKeys, None)

      val event1: Event = ObserveEvent(Prefix("tcs"), EventName("event.key1"))
      val event2: Event = ObserveEvent(Prefix("tcs"), EventName("event.key2"))

      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1, event2)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.subscribe(eventKeys)).thenReturn(eventStream)

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val responseSet = Source(1 to 2)
          .map(_ => decodeMessage[Either[EventError, Event]](wsClient))
          .runWith(Sink.seq)
          .futureValue
          .map(_.rightValue)
          .toSet

        responseSet shouldEqual Set(event1, event2)
      }
    }

    "return set of events on subscribe events when subscribe event is sent with maxFrequency = 10 | ESW-216" in {
      val tcsEventKeyStr1 = "tcs.event.key1"
      val tcsEventKeyStr2 = "tcs.event.key2"
      val eventKey1       = EventKey(tcsEventKeyStr1)
      val eventKey2       = EventKey(tcsEventKeyStr2)
      val eventKeys       = Set(eventKey1, eventKey2)

      val eventSubscriptionRequest = Subscribe(eventKeys, Some(10))

      val event1: Event = ObserveEvent(Prefix("tcs"), EventName("event.key1"))
      val event2: Event = ObserveEvent(Prefix("tcs"), EventName("event.key2"))

      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1, event2)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.subscribe(eventKeys, 100.millis, RateLimiterMode)).thenReturn(eventStream)

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val responseSet = Source(1 to 2)
          .map(_ => decodeMessage[Either[EventError, Event]](wsClient))
          .runWith(Sink.seq)
          .futureValue
          .map(_.rightValue)
          .toSet

        responseSet shouldEqual Set(event1, event2)
      }
    }

    "return InvalidMaxFrequency when subscribe event is sent with maxFrequency <= 0 | ESW-216" in {
      val tcsEventKeyStr1 = "tcs.event.key1"
      val tcsEventKeyStr2 = "tcs.event.key2"
      val eventKey1       = EventKey(tcsEventKeyStr1)
      val eventKey2       = EventKey(tcsEventKeyStr2)
      val eventKeys       = Set(eventKey1, eventKey2)

      val eventSubscriptionRequest = Subscribe(eventKeys, Some(-1))
      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val response = Source(1 to 10)
          .map(_ => decodeMessage[Either[EventError, Event]](wsClient))
          .runWith(Sink.head)
          .leftValue

        response shouldEqual InvalidMaxFrequency()
      }
    }

    "return set of events on subscribe events with a given pattern | ESW-216" in {
      val eventSubscriptionRequest = SubscribeWithPattern(TCS, None, "*")

      val event1: Event = ObserveEvent(Prefix("tcs"), EventName("event.key1"))
      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.pSubscribe(TCS, "*")).thenReturn(eventStream)

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val responseSet = Source(1 to 2)
          .take(1)
          .map(_ => decodeMessage[Either[EventError, Event]](wsClient))
          .runWith(Sink.seq)
          .futureValue
          .map(_.rightValue)
          .toSet

        responseSet shouldEqual Set(event1)

      }
    }

    "return set of events on subscribe events with a given pattern and maxFrequency = 5 | ESW-216" in {
      val eventSubscriptionRequest = SubscribeWithPattern(TCS, Some(5), "*")
      val event1: Event            = ObserveEvent(Prefix("tcs"), EventName("event.key1"))

      val eventSubscription: EventSubscription = new EventSubscription {
        override def unsubscribe(): Future[Done] = Future.successful(Done)

        override def ready(): Future[Done] = Future.successful(Done)
      }

      val eventStream = Source(List(event1)).mapMaterializedValue(_ => eventSubscription)

      when(eventSubscriber.pSubscribe(TCS, "*")).thenReturn(eventStream)
      when(eventSubscriberUtil.subscriptionModeStage(200.millis, RateLimiterMode))
        .thenReturn(new RateLimiterStub[Event](200.millis))

      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val responseSet = Source(1 to 2)
          .take(1)
          .map(_ => decodeMessage[Either[EventError, Event]](wsClient))
          .runWith(Sink.seq)
          .futureValue
          .map(_.rightValue)
          .toSet

        responseSet shouldEqual Set(event1)

      }
    }

    "return InvalidMaxFrequency when subscribe event with pattern is sent with maxFrequency <= 0 | ESW-216" in {
      val eventSubscriptionRequest = SubscribeWithPattern(TCS, Some(-1), "*")
      WS("/websocket", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(eventSubscriptionRequest))
        isWebSocketUpgrade shouldBe true

        val response = Source(1 to 10)
          .map(_ => decodeMessage[Either[InvalidMaxFrequency, Event]](wsClient))
          .runWith(Sink.head)
          .leftValue

        response shouldEqual InvalidMaxFrequency()
      }
    }

  }

  private def decodeMessage[T](wsClient: WSProbe)(implicit decoder: Decoder[T]): T = {
    wsClient.expectMessage() match {
      case TextMessage.Strict(text) => JsonText.decodeText[T](text)
      case _                        => throw new RuntimeException("The expected message is not TextMessage")
    }
  }

}
