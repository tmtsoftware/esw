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
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Id
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class RoutesTest
    extends WordSpec
    with CswContextMocks
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with JsonSupportExt {

  private val routes = new Routes(cswCtx).route

  "Routes for command/assembly" must {
    "post submit command | ESW-91" in {

      val assemblyName = "TestAssembly"
      val runId        = "123"

      val obj = JsObject(
        Seq(
          "type"        -> JsString("Setup"),
          "source"      -> JsString("test"),
          "commandName" -> JsString("c1"),
          "maybeObsId"  -> JsString("o1"),
          "runId"       -> JsString(runId),
          "paramSet"    -> JsArray()
        )
      )

      val controlCommand = JsonSupport.controlCommandFormat.reads(obj).get

      when(commandService.submit(controlCommand)).thenReturn(Future.successful(Completed(Id(runId))))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Post("/command/assembly/" + assemblyName + "/submit", obj) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val expectedResponse = JsObject(Seq("runId" -> JsString(runId), "type" -> JsString("Completed")))
        responseAs[JsObject] shouldEqual expectedResponse
      }
    }

    "post oneway command | ESW-91" in {

      val assemblyName = "TestAssembly"
      val runId        = "123"

      val obj = JsObject(
        Seq(
          "type"        -> JsString("Setup"),
          "source"      -> JsString("test"),
          "commandName" -> JsString("c1"),
          "maybeObsId"  -> JsString("o1"),
          "runId"       -> JsString(runId),
          "paramSet"    -> JsArray()
        )
      )

      val controlCommand = JsonSupport.controlCommandFormat.reads(obj).get

      when(commandService.oneway(controlCommand)).thenReturn(Future.successful(Accepted(Id(runId))))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Post("/command/assembly/" + assemblyName + "/oneway", obj) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val expectedResponse = JsObject(Seq("runId" -> JsString(runId), "type" -> JsString("Accepted")))
        responseAs[JsObject] shouldEqual expectedResponse
      }
    }

    "get command response for RunId | ESW-91" in {

      val assemblyName = "TestAssembly"
      val runId        = "123"

      val obj = JsObject(
        Seq(
          "type"        -> JsString("Setup"),
          "source"      -> JsString("test"),
          "commandName" -> JsString("c1"),
          "maybeObsId"  -> JsString("o1"),
          "runId"       -> JsString(runId),
          "paramSet"    -> JsArray()
        )
      )

      when(commandService.queryFinal(Id(runId))(Timeout(100.hours))).thenReturn(Future.successful(Completed(Id(runId))))
      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))

      Get("/command/assembly/" + assemblyName + "/123", obj) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        mediaType shouldBe `text/event-stream`

        val expectedEntity = JsObject(
          Seq(
            "runId" -> JsString(runId),
            "type"  -> JsString("Completed")
          )
        )

        val actualDataF = responseAs[Source[ServerSentEvent, NotUsed]]
          .map(sse => Json.parse(sse.getData()))
          .runWith(Sink.seq)

        Await.result(actualDataF, 5.seconds) shouldEqual Seq(expectedEntity)
      }
    }

//    "get error response for command on timeout | ESW-91" in {
//
//      val assemblyName = "TestAssembly"
//      val runId        = "123"
//
//      when(commandService.queryFinal(Id(runId))).thenReturn(Future.failed(new TimeoutException("")))
//      when(componentFactory.assemblyCommandService(assemblyName)).thenReturn(Future(commandService))
//
//      Get("/assembly/" + assemblyName + "/queryFinal?runId=123&timeout=5000") ~> routes ~> check {
//        status shouldBe StatusCodes.OK
//        mediaType shouldBe `text/event-stream`
//
//        val expectedEntity = JsObject(
//          Seq(
//            "runId" -> JsString(runId),
//            "type"  -> JsString("Completed")
//          )
//        )
//
//        val actualDataF = responseAs[Source[ServerSentEvent, NotUsed]]
//          .map(sse => Json.parse(sse.getData()))
//          .runWith(Sink.seq)
//
//        actualDataF.onComplete(println)
//
//
//      }
//    }
  }

}
