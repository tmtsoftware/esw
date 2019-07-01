package esw.gateway.server.routes

import akka.NotUsed
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `text/event-stream`}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import csw.command.api.CurrentStateSubscription
import csw.location.api.models.ComponentType
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName, StateVariable}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.gateway.server.CswContextMocks
import esw.template.http.server.commons.JsonSupportExt
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchersSugar, Mockito}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future, TimeoutException}

class CommandRoutesTest
    extends WordSpec
    with CswContextMocks
    with Matchers
    with ArgumentMatchersSugar
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with PlayJsonSupport
    with JsonSupportExt {

  import actorRuntime.timeout

  override protected def afterAll(): Unit  = cswCtx.actorSystem.terminate()
  override protected def afterEach(): Unit = Mockito.reset(componentFactory, commandService)

  case class TestData(componentType: String)

  val testData = List(TestData("hcd"), TestData("assembly"))

  testData.foreach { testData =>
    val componentType: ComponentType = ComponentType.withName(testData.componentType)

    s"CommandRoutes for ${testData.componentType}" must {
      "post command to validate | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        val command       = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

        when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))
        Post(s"/command/${testData.componentType}/$componentName/validate", command) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[CommandResponse] shouldEqual Accepted(runId)
        }
      }

      "get error response for validate command on timeout | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        val command       = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

        when(commandService.validate(command)).thenReturn(Future.failed(new TimeoutException("")))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Post(s"/command/${testData.componentType}/$componentName/validate", command) ~> route ~> check {
          status shouldBe StatusCodes.GatewayTimeout
          mediaType shouldBe `application/json`
        }
      }

      "post submit command | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        val command       = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

        when(commandService.submit(command)).thenReturn(Future.successful(Completed(runId)))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Post(s"/command/${testData.componentType}/$componentName/submit", command) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[CommandResponse] shouldEqual Completed(runId)
        }
      }

      "get error response for submit command on timeout | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        val command       = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

        when(commandService.submit(command)).thenReturn(Future.failed(new TimeoutException("")))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Post(s"/command/${testData.componentType}/$componentName/submit", command) ~> route ~> check {
          status shouldBe StatusCodes.GatewayTimeout
          mediaType shouldBe `application/json`
        }
      }

      "post oneway command | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        val command       = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

        when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Post(s"/command/${testData.componentType}/$componentName/oneway", command) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[CommandResponse] shouldEqual Accepted(runId)
        }
      }

      "get error response for oneway command on timeout | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        val command       = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

        when(commandService.oneway(command)).thenReturn(Future.failed(new TimeoutException("")))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Post(s"/command/${testData.componentType}/$componentName/oneway", command) ~> route ~> check {
          status shouldBe StatusCodes.GatewayTimeout
          mediaType shouldBe `application/json`
        }
      }

      "get command response for given RunId | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")

        when(commandService.queryFinal(any[Id])(any[Timeout])).thenReturn(Future.successful(Completed(runId)))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Get(s"/command/${testData.componentType}/$componentName/${runId.id}") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe `text/event-stream`

          val actualDataF: Future[Seq[CommandResponse]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[CommandResponse](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(Completed(runId))
        }
      }

      "get error response for command on timeout | ESW-91" in {
        val componentName = "test-component"
        val runId         = Id("123")
        when(commandService.queryFinal(any[Id])(any[Timeout])).thenReturn(Future.failed(new TimeoutException("")))
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Get(s"/command/${testData.componentType}/$componentName/${runId.id}") ~> route ~> check {
          status shouldBe StatusCodes.GatewayTimeout
          mediaType shouldBe `application/json`
        }
      }

      "get current state subscription to all stateNames | ESW-91" in {
        val componentName = "test-component"
        val currentState1 = CurrentState(Prefix("a.b"), StateName("stateName1"))
        val currentState2 = CurrentState(Prefix("a.b"), StateName("stateName2"))

        val currentStateSubscription = mock[CurrentStateSubscription]

        val currentStateStream = Source(List(currentState1, currentState2))
          .mapMaterializedValue(_ => currentStateSubscription)

        when(commandService.subscribeCurrentState(Set.empty[StateName])).thenReturn(currentStateStream)
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Get(s"/command/${testData.componentType}/$componentName/current-state/subscribe") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe `text/event-stream`

          val actualDataF: Future[Seq[StateVariable]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[StateVariable](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(currentState1, currentState2)
        }
      }

      "get current state subscription to given stateNames | ESW-91" in {
        val componentName = "test-component"
        val stateName1    = StateName("stateName1")
        val currentState1 = CurrentState(Prefix("a.b"), stateName1)

        val currentStateSubscription = mock[CurrentStateSubscription]

        val currentStateStream = Source(List(currentState1))
          .mapMaterializedValue(_ => currentStateSubscription)

        when(commandService.subscribeCurrentState(Set(stateName1))).thenReturn(currentStateStream)
        when(componentFactory.commandService(componentName, componentType)).thenReturn(Future(commandService))

        Get(s"/command/${testData.componentType}/$componentName/current-state/subscribe?stateName=${stateName1.name}") ~> route ~> check {
          status shouldBe StatusCodes.OK
          mediaType shouldBe `text/event-stream`

          val actualDataF: Future[Seq[StateVariable]] = responseAs[Source[ServerSentEvent, NotUsed]]
            .map(sse => Json.fromJson[StateVariable](Json.parse(sse.getData())).get)
            .runWith(Sink.seq)

          Await.result(actualDataF, 5.seconds) shouldEqual Seq(currentState1)
        }
      }
    }
  }
}
