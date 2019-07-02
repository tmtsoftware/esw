package esw.template.http.server

import _root_.csw.params.core.formats.JsonSupport
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

trait HttpTestSuite extends BaseTestSuite with ScalatestRouteTest with JsonSupport with PlayJsonSupport {
  override protected def afterAll(): Unit = {
    // shuts down the ScalaRouteTest ActorSystem
    cleanUp()
  }
}
