package esw.http.core

import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.location.client.HttpCodecs
import csw.params.core.formats.ParamCodecs

trait HttpTestSuite extends BaseTestSuite with ScalatestRouteTest with ParamCodecs with HttpCodecs {

  // fixme: do we really need this? ScalatestRouteTest already does this
  override protected def afterAll(): Unit = {
    // shuts down the ScalaRouteTest ActorSystem
    cleanUp()
  }
}
