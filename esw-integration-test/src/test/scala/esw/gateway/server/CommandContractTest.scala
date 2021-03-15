package esw.gateway.server

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.Sink
import csw.event.client.EventServiceFactory
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.{Accepted, Completed, Started}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.ObsId
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.testkit.scaladsl.CSWService.EventServer
import esw.gateway.api.clients.ClientFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.server.testdata.AssemblyBehaviourFactory
import esw.gateway.server.testdata.SampleAssemblyHandlers._
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.Gateway

import scala.concurrent.Future

class CommandContractTest extends EswTestKit(EventServer, Gateway) with GatewayCodecs {

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnAssembly(Prefix("ESW.test"), new AssemblyBehaviourFactory())
  }

  "CommandApi" must {

    val prefix = Prefix("esw.test")
    "handle validate, oneway, submit, subscribe current state and queryFinal commands | ESW-223, ESW-100, ESW-91, ESW-216, ESW-86, ESW-98, CSW-81" in {
      // gatewayPostClient and gatewayWsClient requires gateway location which is resolved using Location Service in EswTestKit
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val eventKey     = EventKey(Prefix("tcs.filter.wheel"), EventName("setup-command-from-script"))

      val command            = Setup(prefix, CommandName("c1"), Some(ObsId("2020A-001-123")))
      val longRunningCommand = Setup(prefix, CommandName("long-running"), Some(ObsId("2020A-001-123")))
      val componentId        = ComponentId(prefix, Assembly)
      val stateNames         = Set(StateName("stateName1"), StateName("stateName2"))
      val currentState1      = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2      = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val commandService                            = clientFactory.component(componentId)
      val currentStatesF: Future[Seq[CurrentState]] = commandService.subscribeCurrentState(stateNames).take(2).runWith(Sink.seq)
      Thread.sleep(1000)

      //validate
      commandService.validate(command).futureValue shouldBe an[Accepted]
      //oneway
      commandService.oneway(command).futureValue shouldBe an[Accepted]

      //submit-setup-command-subscription
      val testProbe    = TestProbe[Event]()
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      //submit the setup command
      val submitResponse = commandService.submit(longRunningCommand).futureValue
      submitResponse shouldBe a[Started]

      val actualSetupEvent: SystemEvent = testProbe.expectMessageType[SystemEvent]

      //assert the event which is publish in onSubmit handler of component
      actualSetupEvent.eventKey should ===(eventKey)

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))

      //queryFinal
      commandService.queryFinal(submitResponse.runId).futureValue should ===(Completed(submitResponse.runId))
    }

    "handle submitAndWait command | ESW-223, ESW-100, ESW-91, ESW-216, ESW-86, CSW-81" in {
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)
      val eventService  = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

      val longRunningCommand = Setup(prefix, CommandName("long-running"), Some(ObsId("2020A-001-123")))
      val componentId        = ComponentId(prefix, Assembly)

      val commandService = clientFactory.component(componentId)

      //submit-setup-command-subscription
      val testProbe    = TestProbe[Event]()
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent]

      //submit the setup command
      val submitResponseF = commandService.submitAndWait(longRunningCommand)
      extractResponse(testProbe.expectMessageType[SystemEvent]) should ===("Started")
      submitResponseF.futureValue shouldBe a[Completed]
      extractResponse(testProbe.expectMessageType[SystemEvent]) should ===("Completed")
    }

    "handle large websocket requests | CSW-81" in {
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

      val componentType = Assembly
      val command       = Setup(prefix, CommandName("c1"), Some(ObsId("2020A-001-123")))
      val componentId   = ComponentId(prefix, componentType)
      val stateNames    = (1 to 10000).toSet[Int].map(x => StateName(s"stateName$x"))
      val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val commandService                            = clientFactory.component(componentId)
      val currentStatesF: Future[Seq[CurrentState]] = commandService.subscribeCurrentState(stateNames).take(2).runWith(Sink.seq)
      Thread.sleep(500)

      //oneway
      commandService.oneway(command).futureValue shouldBe an[Accepted]

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))
    }
  }
}
