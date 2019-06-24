package esw.gateway.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Id
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsArray, JsObject, JsString}

import scala.concurrent.Future

class RoutesTest extends WordSpec with CswContextMocks with Matchers with ScalatestRouteTest with PlayJsonSupport {

  private val routes = new Routes(cswCtx).route

  "Routes for assembly" must {
    "send submit command | ESW-91" in {

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

      Post("/assembly/" + assemblyName + "/submit", obj) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val expectedResponse = JsObject(Seq("runId" -> JsString(runId), "type" -> JsString("Completed")))
        responseAs[JsObject] shouldEqual expectedResponse
      }
    }

    "send oneway command | ESW-91" in {

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

      Post("/assembly/" + assemblyName + "/oneway", obj) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val expectedResponse = JsObject(Seq("runId" -> JsString(runId), "type" -> JsString("Accepted")))
        responseAs[JsObject] shouldEqual expectedResponse
      }
    }
  }

}
