package esw.gateway.server

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import csw.event.client.EventServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Accepted, Completed, Started}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.gateway.api.clients.CommandClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.http.core.FutureEitherExt
import msocket.api.Transport
import msocket.impl.Encoding.JsonText
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport
import org.scalatest.WordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandGatewayTest
    extends ScalaTestFrameworkTestKit(EventServer)
    with WordSpecLike
    with FutureEitherExt
    with GatewayCodecs {

  import frameworkTestKit._

  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = actorSystem
  private implicit val mat: Materializer                               = Materializer.matFromSystem(typedSystem)
  private val port: Int                                                = 6490
  private val gatewayWiring: GatewayWiring                             = new GatewayWiring(Some(port))

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
  }

  override protected def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "CommandApi" must {

    "handle validate, oneway, submit, subscribe current state and queryFinal commands | ESW-223, ESW-100, ESW-91, ESW-216, ESW-86" in {
      val postClient: Transport[PostRequest] =
        new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint", JsonText)
      val commandClient = new CommandClient(postClient, websocketClient)

      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val eventKey     = EventKey(Prefix("tcs.filter.wheel"), EventName("setup-command-from-script"))

      val componentName      = "test"
      val componentType      = Assembly
      val command            = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId")))
      val longRunningCommand = Setup(Prefix("esw.test"), CommandName("long-running"), Some(ObsId("obsId")))
      val componentId        = ComponentId(componentName, componentType)
      val stateNames         = Set(StateName("stateName1"), StateName("stateName2"))
      val currentState1      = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2      = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStatesF: Future[Seq[CurrentState]] =
        commandClient.subscribeCurrentState(componentId, stateNames, None).take(2).runWith(Sink.seq)
      Thread.sleep(1000)

      //validate
      commandClient.validate(componentId, command).rightValue shouldBe an[Accepted]
      //oneway
      commandClient.oneway(componentId, command).rightValue shouldBe an[Accepted]

      //submit-setup-command-subscription
      val testProbe    = TestProbe[Event]
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      //submit the setup command
      val submitResponse = commandClient.submit(componentId, longRunningCommand).rightValue
      submitResponse shouldBe a[Started]

      val actualSetupEvent: SystemEvent = testProbe.expectMessageType[SystemEvent]

      //assert the event which is publish in onSubmit handler of component
      actualSetupEvent.eventKey should ===(eventKey)

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))

      //queryFinal
      commandClient.queryFinal(componentId, submitResponse.runId).rightValue should ===(Completed(submitResponse.runId))
    }

    "handle large websocket requests" in {
      val postClient: Transport[PostRequest] =
        new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint", JsonText)
      val commandClient = new CommandClient(postClient, websocketClient)

      val componentName = "test"
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId")))
      val componentId   = ComponentId(componentName, componentType)
      val stateNames    = (1 to 10000).toSet[Int].map(x => StateName(s"stateName$x"))
      val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStatesF: Future[Seq[CurrentState]] =
        commandClient.subscribeCurrentState(componentId, stateNames, None).take(2).runWith(Sink.seq)
      Thread.sleep(500)

      //oneway
      commandClient.oneway(componentId, command).futureValue.rightValue shouldBe an[Accepted]

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))
    }
  }

}
