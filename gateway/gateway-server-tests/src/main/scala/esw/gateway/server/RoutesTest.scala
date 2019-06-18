package esw.gateway.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

class RoutesTest extends FunSuite with Matchers with ScalatestRouteTest {

  private val wiring = new Wiring(None)

  import wiring.routes.route

  test("get - success status code") {

    Get("/hello") ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
