package esw.gateway.server

import akka.NotUsed
import akka.http.scaladsl.model.MediaTypes.`text/event-stream`
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.ArgumentMatchersSugar
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future, TimeoutException}

class RoutesTest
    extends WordSpec
    with CswContextMocks
    with Matchers
    with ArgumentMatchersSugar
    with ScalatestRouteTest
    with PlayJsonSupport
    with JsonSupportExt {

  private val routes = new Routes(cswCtx).route

  import actorRuntime.timeout

  "Routes for command/assembly" must {
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
//        mediaType shouldBe `text/event-stream`
//
//        val actualDataF: Future[Seq[CommandResponse]] = responseAs[Source[ServerSentEvent, NotUsed]]
//          .map(sse => Json.fromJson[CommandResponse](Json.parse(sse.getData())).get)
//          .runWith(Sink.seq)
//
//        Await.result(actualDataF, 5.seconds) shouldEqual Seq(Completed(runId))

      }
    }
  }

}
