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
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName, StateVariable}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.gateway.server.{CswContextMocks, JsonSupportExt}
import org.mockito.ArgumentMatchersSugar
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
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
    with PlayJsonSupport
    with JsonSupportExt {

  private val routes = new CommandRoutes(cswCtx).route

  import actorRuntime.timeout

  override protected def afterAll(): Unit = cswCtx.actorSystem.terminate()

  "Routes for assembly" must {
    "post submit command | ESW-91" in {
      val assemblyName = "TestAssembly"
      val runId        = Id("123")
      val command      = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.submit(command)).thenReturn(Future.successful(Completed(runId)))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Post(s"/command/assembly/$assemblyName/submit", command) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[CommandResponse] shouldEqual Completed(runId)
      }
    }

    "post oneway command | ESW-91" in {
      val assemblyName = "TestAssembly"
      val runId        = Id("123")
      val command      = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Post(s"/command/assembly/$assemblyName/oneway", command) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[CommandResponse] shouldEqual Accepted(runId)
      }
    }

    "get command response for RunId | ESW-91" in {
      val assemblyName = "TestAssembly"
      val runId        = Id("123")
      val command      = Setup(Prefix("test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)

      when(commandService.queryFinal(any[Id])(any[Timeout])).thenReturn(Future.successful(Completed(runId)))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Get(s"/command/assembly/$assemblyName/${runId.id}", command) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val actualDataF: Future[Seq[CommandResponse]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.fromJson[CommandResponse](Json.parse(sse.getData())).get)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(Completed(runId))
      }
    }

    "get error response for command on timeout | ESW-91" in {
      val assemblyName = "TestAssembly"
      val runId        = Id("123")
      when(commandService.queryFinal(any[Id])(any[Timeout])).thenReturn(Future.failed(new TimeoutException("")))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Get(s"/command/assembly/$assemblyName/${runId.id}") ~> routes ~> check {
        status shouldBe StatusCodes.GatewayTimeout
        mediaType shouldBe `application/json`
      }
    }

    "get current state subscription to all state-names | ESW-91" in {
      val assemblyName  = "TestAssembly"
      val currentState1 = CurrentState(Prefix("a.b"), StateName("stateName1"))
      val currentState2 = CurrentState(Prefix("a.b"), StateName("stateName2"))

      val currentStateSubscription = mock[CurrentStateSubscription]

      val currentStateStream = Source(List(currentState1, currentState2))
        .mapMaterializedValue(_ => currentStateSubscription)

      when(commandService.subscribeCurrentState(Set.empty[StateName])).thenReturn(currentStateStream)
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Get(s"/command/assembly/$assemblyName/current-state/subscribe") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val actualDataF: Future[Seq[StateVariable]] = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.fromJson[StateVariable](Json.parse(sse.getData())).get)
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(currentState1, currentState2)
      }
    }
  }

}
