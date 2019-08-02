package esw.ocs.utils

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ComponentType.Sequencer
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.{Prefix, Subsystem}
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.messages.RegistrationError
import org.mockito.Mockito.{verify, when}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LocationServiceUtilsTest extends BaseTestSuite {

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

      val locationServiceUtils = new LocationServiceUtils(locationService)

      locationServiceUtils.register(registration)(coordinatedShutdown).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val errorMsg            = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      val locationServiceUtils = new LocationServiceUtils(locationService)

      locationServiceUtils.register(registration)(coordinatedShutdown).leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate().futureValue
    }
  }

  "registerWithRetry" must {
    "return successful RegistrationResult | ESW-144" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val retryCount          = 2
      val registrationResult  = mock[RegistrationResult]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration)).thenReturn(Future(registrationResult))

      val locationServiceUtils = new LocationServiceUtils(locationService)

      locationServiceUtils
        .registerWithRetry(registration, retryCount)(coordinatedShutdown)
        .rightValue should ===(
        akkaLocation
      )
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "retry if OtherLocationIsRegistered | ESW-144" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val errorMsg            = "error message"
      val retryCount          = 1
      val registrationResult  = mock[RegistrationResult]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration))
        .thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)), Future(registrationResult))

      val locationServiceUtils = new LocationServiceUtils(locationService)

      locationServiceUtils
        .registerWithRetry(registration, retryCount)(coordinatedShutdown)
        .rightValue should ===(
        akkaLocation
      )
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "not retry if RegistrationFailed | ESW-144" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val errorMsg            = "error message"
      val retryCount          = 3
      val registrationResult  = mock[RegistrationResult]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration))
        .thenReturn(Future.failed(RegistrationFailed(errorMsg)), Future(registrationResult))

      val locationServiceUtils = new LocationServiceUtils(locationService)

      locationServiceUtils
        .registerWithRetry(registration, retryCount)(coordinatedShutdown)
        .leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate().futureValue
    }

    "map location service registration failure to RegistrationError if could not register after retry attempts | ESW-144" in {
      val system              = ActorSystem("test")
      val coordinatedShutdown = CoordinatedShutdown(system)
      val errorMsg            = "error message"
      val retryCount          = 2
      when(locationService.register(registration))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg))
        )

      val locationServiceUtils = new LocationServiceUtils(locationService)

      locationServiceUtils
        .registerWithRetry(registration, retryCount)(coordinatedShutdown)
        .leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate().futureValue
    }

  }

  "listBySubsystem" must {
    "list all locations which match given componentType and subsystem" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", ComponentType.SequenceComponent)), Prefix("tcs.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_2", ComponentType.SequenceComponent)), Prefix("tcs.test.filter2"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_3", ComponentType.SequenceComponent)), Prefix("tcs.test.filter3"), testUri)
      )
      val sequenceComponentLocations = tcsLocations ++ List(
        AkkaLocation(AkkaConnection(ComponentId("OSS_1", ComponentType.SequenceComponent)), Prefix("oss.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("IRIS_1", ComponentType.SequenceComponent)), Prefix("iris.test.filter1"), testUri)
      )

      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtils = new LocationServiceUtils(locationService)

      val actualLocations = locationServiceUtils.listBy(Subsystem.TCS, ComponentType.SequenceComponent).futureValue

      actualLocations shouldEqual tcsLocations
    }

    "return empty list if no matching component type and subsystem is found" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", ComponentType.SequenceComponent)), Prefix("tcs.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_2", ComponentType.SequenceComponent)), Prefix("tcs.test.filter2"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("IRIS_1", ComponentType.SequenceComponent)), Prefix("iris.test.filter1"), testUri)
      )

      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtils = new LocationServiceUtils(locationService)

      val actualLocations = locationServiceUtils.listBy(Subsystem.NFIRAOS, ComponentType.SequenceComponent).futureValue

      actualLocations shouldEqual List.empty
    }
  }
}
