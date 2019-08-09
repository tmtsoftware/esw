package esw.gateway.server.routes

import akka.NotUsed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `text/event-stream`}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.testkit.WSProbe
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import csw.command.api.CurrentStateSubscription
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.{Accepted, Completed, SubmitResponse}
import csw.params.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName, StateVariable}
import esw.gateway.server.CswContextMocks
import esw.http.core.HttpTestSuite
import io.bullet.borer.Json

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future, TimeoutException}

class CommandRoutesTest extends HttpTestSuite {
  val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  trait Setup {
    val cswMocks                  = new CswContextMocks(actorSystem)
    implicit val timeout: Timeout = 5.seconds
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  val assembly                    = "assembly"
  val hcd                         = "hcd"
  val assemblyType: ComponentType = ComponentType.withName("assembly")
  val hcdType: ComponentType      = ComponentType.withName("hcd")

  s"POST /command/{componentType}/{componentName}/validate" must {
    "validate the command and return ValidateResponse | ESW-91" in new Setup {

      import cswMocks._
      private val componentName           = "test-component"
      private val runId                   = Id("123")
      private val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))
      when(componentFactory.commandService(componentName, hcdType)).thenReturn(Future(commandService))
      Post(s"/command/$hcd/$componentName/validate", command) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[CommandResponse] shouldEqual Accepted(runId)
      }
    }

    "return GatewayTimeout when request take long time | ESW-91" in new Setup {

      import cswMocks._
      private val componentName           = "test-component"
      private val runId                   = Id("123")
      private val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.validate(command)).thenReturn(Future.failed(new TimeoutException("")))
      when(componentFactory.commandService(componentName, assemblyType)).thenReturn(Future(commandService))

      Post(s"/command/$assembly/$componentName/validate", command) ~> route ~> check {
        status shouldBe StatusCodes.GatewayTimeout
        mediaType shouldBe `application/json`
      }
    }
  }

  s"POST /command/{componentType}/{componentName}/submit" must {
    "submit the given command to command service and return SubmitResponse | ESW-91" in new Setup {

      import cswMocks._
      private val componentName           = "test-component"
      private val runId                   = Id("123")
      private val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.submit(command)).thenReturn(Future.successful(Completed(runId)))
      when(componentFactory.commandService(componentName, hcdType)).thenReturn(Future(commandService))

      Post(s"/command/$hcd/$componentName/submit", command) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[CommandResponse] shouldEqual Completed(runId)
      }
    }

    "return GatewayTimeout when request take long time | ESW-91" in new Setup {

      import cswMocks._
      private val componentName           = "test-component"
      private val runId                   = Id("123")
      private val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.submit(command)).thenReturn(Future.failed(new TimeoutException("")))
      when(componentFactory.commandService(componentName, assemblyType)).thenReturn(Future(commandService))

      Post(s"/command/$assembly/$componentName/submit", command) ~> route ~> check {
        status shouldBe StatusCodes.GatewayTimeout
        mediaType shouldBe `application/json`
      }
    }
  }

  s"POST /command/{componentType}/{componentName}/oneway" must {
    "submit oneway command to command service and return OnewayResponse | ESW-91" in new Setup {

      import cswMocks._
      private val componentName           = "test-component"
      private val runId                   = Id("123")
      private val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))
      when(componentFactory.commandService(componentName, hcdType)).thenReturn(Future(commandService))

      Post(s"/command/$hcd/$componentName/oneway", command) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[CommandResponse] shouldEqual Accepted(runId)
      }
    }

    "return GatewayTimeout when request take long time | ESW-91" in new Setup {

      import cswMocks._
      private val componentName           = "test-component"
      private val runId                   = Id("123")
      private val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.oneway(command)).thenReturn(Future.failed(new TimeoutException("")))
      when(componentFactory.commandService(componentName, assemblyType)).thenReturn(Future(commandService))

      Post(s"/command/$assembly/$componentName/oneway", command) ~> route ~> check {
        status shouldBe StatusCodes.GatewayTimeout
        mediaType shouldBe `application/json`
      }
    }
  }

  s"GET /command/{componentType}/{componentName}/{runId}" must {

    "return a stream which finishes with CommandResponse | ESW-91" in new Setup {

      import cswMocks._
      private val componentName = "test-component"
      private val runId         = Id("123")

      when(commandService.queryFinal(any[Id])(any[Timeout])).thenReturn(Future.successful(Completed(runId)))
      when(componentFactory.commandService(componentName, hcdType)).thenReturn(Future(commandService))

      Get(s"/command/$hcd/$componentName/${runId.id}") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val actualDataF: Future[Seq[CommandResponse]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[CommandResponse].value)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(Completed(runId))
      }
    }
  }

  "GET /command/{componentType}/{componentName}/current-state/subscribe" must {

    "return a stream of current state of the component | ESW-91" in new Setup {

      import cswMocks._

      private val componentName = "test-component"
      private val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      private val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      private val currentStateSubscription = mock[CurrentStateSubscription]

      private val currentStateStream = Source(List(currentState1, currentState2))
        .mapMaterializedValue(_ => currentStateSubscription)

      when(commandService.subscribeCurrentState(Set.empty[StateName])).thenReturn(currentStateStream)
      when(componentFactory.commandService(componentName, assemblyType)).thenReturn(Future(commandService))

      Get(s"/command/$assembly/$componentName/current-state/subscribe") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val actualDataF: Future[Seq[StateVariable]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[StateVariable].value)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(currentState1, currentState2)
      }
    }

    "return a stream of current state of the component with state name filter | ESW-91" in new Setup {

      import cswMocks._

      val componentName = "test-component"
      val stateName1    = StateName("stateName1")
      val currentState1 = CurrentState(Prefix("esw.a.b"), stateName1)

      private val currentStateSubscription = mock[CurrentStateSubscription]

      private val currentStateStream = Source(List(currentState1))
        .mapMaterializedValue(_ => currentStateSubscription)

      when(commandService.subscribeCurrentState(Set(stateName1))).thenReturn(currentStateStream)
      when(componentFactory.commandService(componentName, hcdType)).thenReturn(Future(commandService))

      Get(s"/command/$hcd/$componentName/current-state/subscribe?state-name=${stateName1.name}") ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val actualDataF: Future[Seq[StateVariable]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[StateVariable].value)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(currentState1)
      }
    }

    "return a stream of current state of the component with given max-frequency | ESW-91" in new Setup {

      import cswMocks._
      private val componentName = "test-component"
      private val stateName1    = StateName("stateName1")
      private val currentState1 = CurrentState(Prefix("esw.a.b"), stateName1)

      private val currentStateSubscription = mock[CurrentStateSubscription]

      private val currentStateStream = Source(List(currentState1))
        .mapMaterializedValue(_ => currentStateSubscription)

      when(commandService.subscribeCurrentState(Set(stateName1))).thenReturn(currentStateStream)
      when(componentFactory.commandService(componentName, assemblyType)).thenReturn(Future(commandService))

      Get(
        s"/command/$assembly/$componentName/current-state/subscribe?state-name=${stateName1.name}&max-frequency=10"
      ) ~> route ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val actualDataF: Future[Seq[StateVariable]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.decode(sse.getData().getBytes("utf8")).to[StateVariable].value)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(currentState1)
      }
    }
  }

  "WS /command/{componentType}/{componentName}/websocket/{runId}/" must {
    "return a stream which finishes with CommandResponse" in new Setup {
      import cswMocks._
      import io.bullet.borer.compat.akka._

      val wsClient         = WSProbe()
      val runId            = Id("123")
      val componentName    = "sample"
      val expectedResponse = Completed(runId)

      when(commandService.queryFinal(any[Id])(any[Timeout])).thenReturn(Future.successful(expectedResponse))
      when(componentFactory.commandService(componentName, assemblyType)).thenReturn(Future(commandService))

      WS(s"/command/${assembly}/${componentName}/websocket/${runId.id}", wsClient.flow) ~> route ~> check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true
        val response: CommandResponse =
          Json.decode(wsClient.expectMessage().asBinaryMessage.getStrictData).to[SubmitResponse].value

        response shouldEqual expectedResponse
      }
    }
  }
}
