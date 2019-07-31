package esw.ocs.utils

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ComponentType.Sequencer
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId}
import csw.params.core.models.Prefix
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.messages.RegistrationError
import org.mockito.Mockito.{verify, when}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationUtilsTest extends BaseTestSuite {

  private val locationService = mock[LocationService]

  private val prefix         = Prefix("tcs.home.datum")
  private val uri            = new URI("uri")
  private val akkaConnection = AkkaConnection(ComponentId("ocs", Sequencer))
  private val registration   = AkkaRegistration(akkaConnection, prefix, uri)
  private val akkaLocation   = AkkaLocation(akkaConnection, prefix, uri)

  "register" must {
    "return successful RegistrationResult" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val registrationResult  = mock[RegistrationResult]
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration)).thenReturn(Future(registrationResult))
      RegistrationUtils.register(locationService, registration)(coordinatedShutdown).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val errorMsg            = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))
      RegistrationUtils.register(locationService, registration)(coordinatedShutdown).leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate().futureValue
    }
  }
}
