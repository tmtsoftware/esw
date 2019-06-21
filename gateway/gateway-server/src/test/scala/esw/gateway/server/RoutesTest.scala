package esw.gateway.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

class RoutesTest extends FunSuite with Matchers with ScalatestRouteTest {

  private val route = new Routes().route

  test("ESW-86 | get - success status code") {

    Get("/hello") ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
