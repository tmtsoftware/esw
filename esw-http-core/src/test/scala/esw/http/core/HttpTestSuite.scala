package esw.http.core

import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.commons.http.{ErrorMessage, ErrorResponse}
import csw.location.client.HttpCodecs
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass

trait HttpTestSuite extends BaseTestSuite with ScalatestRouteTest with ParamCodecs with HttpCodecs {

  implicit lazy val ErrorResponseCodec: Codec[ErrorResponse] = deriveCodecForUnaryCaseClass[ErrorResponse]
  implicit lazy val ErrorMessage: Codec[ErrorMessage]        = deriveCodec[ErrorMessage]

  // fixme: do we really need this? ScalatestRouteTest already does this
  override protected def afterAll(): Unit = {
    // shuts down the ScalaRouteTest ActorSystem
    cleanUp()
  }
}
