package esw.dsl.sequence_manager

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ComponentType._
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId}
import csw.params.core.models.{Prefix, Subsystem}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.RegistrationError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LocationServiceUtilTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val locationService = mock[LocationService]

  private val prefix         = Prefix("tcs.home.datum")
  private val uri            = new URI("uri")
  private val akkaConnection = AkkaConnection(ComponentId("ocs", Sequencer))
  private val registration   = AkkaRegistration(akkaConnection, prefix, uri)
  private val akkaLocation   = AkkaLocation(akkaConnection, prefix, uri)

  "register" must {
    "return successful RegistrationResult | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val coordinatedShutdown             = CoordinatedShutdown(system.toUntyped)
      val registrationResult              = mock[RegistrationResult]
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration)).thenReturn(Future(registrationResult))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      locationServiceDsl.register(registration).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      locationServiceDsl.register(registration).leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate()
    }
  }

  "listBySubsystem" must {
    "list all locations which match given componentType and subsystem | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", SequenceComponent)), Prefix("tcs.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_2", SequenceComponent)), Prefix("tcs.test.filter2"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_3", SequenceComponent)), Prefix("tcs.test.filter3"), testUri)
      )
      val sequenceComponentLocations = tcsLocations ++ List(
        AkkaLocation(AkkaConnection(ComponentId("OSS_1", SequenceComponent)), Prefix("oss.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("IRIS_1", SequenceComponent)), Prefix("iris.test.filter1"), testUri)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceDsl.listBy(Subsystem.TCS, SequenceComponent).futureValue

      actualLocations should ===(tcsLocations)
    }

    "return empty list if no matching component type and subsystem is found | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", SequenceComponent)), Prefix("tcs.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_2", SequenceComponent)), Prefix("tcs.test.filter2"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("IRIS_1", SequenceComponent)), Prefix("iris.test.filter1"), testUri)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceDsl.listBy(Subsystem.NFIRAOS, SequenceComponent).futureValue

      actualLocations should ===(List.empty)
    }
  }

  "listByComponentName" must {
    "return all locations which match a given name substring | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS@obsMode1", Sequencer)), Prefix("tcs.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", SequenceComponent)), Prefix("tcs.test.filter2"), testUri)
      )
      val ocsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("OCS@obsMode1", Sequencer)), Prefix("esw.test.filter"), testUri)
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocations ++ ocsLocations))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations    = locationServiceDsl.listByComponentName("TCS").futureValue

      actualLocations should ===(tcsLocations)
    }

    "return all locations which match a given observing mode | ESW-215" in {
      val testUri = new URI("test-uri")
      val obsMode1locations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS@obsMode1", Sequencer)), Prefix("tcs.test.filter1"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("OCS@obsMode1", Sequencer)), Prefix("esw.test.filter"), testUri)
      )
      val obsMode2Locations = List(
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", SequenceComponent)), Prefix("tcs.test.filter2"), testUri)
      )
      when(locationService.list).thenReturn(Future.successful(obsMode1locations ++ obsMode2Locations))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations    = locationServiceDsl.listByComponentName("obsMode1").futureValue

      actualLocations should ===(obsMode1locations)
    }
  }

  "resolveByComponentNameAndType" must {
    "return a location which matches a given component name and type | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(AkkaConnection(ComponentId("TCS@obsMode1", Sequencer)), Prefix("tcs.test.filter1"), testUri)
      val ocsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId("OCS_1", SequenceComponent)), Prefix("esw.test.filter"), testUri),
        AkkaLocation(AkkaConnection(ComponentId("TCS_1", SequenceComponent)), Prefix("tcs.test.filter2"), testUri)
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))
      when(locationService.list(Sequencer)).thenReturn(Future.successful(List(tcsLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceDsl.resolveByComponentNameAndType("TCS@obsMode1", Sequencer).futureValue
      actualLocations.get should ===(tcsLocation)
    }

    "return an IllegalArgumentException when no matching component name and type is found | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(
          AkkaConnection(ComponentId("TCS@obsMode1", Sequencer)),
          Prefix("tcs.test.filter1"),
          testUri
        )
      val ocsLocations = List(
        AkkaLocation(
          AkkaConnection(ComponentId("OCS_1", SequenceComponent)),
          Prefix("esw.test.filter"),
          testUri
        ),
        AkkaLocation(
          AkkaConnection(ComponentId("TCS_1", SequenceComponent)),
          Prefix("tcs.test.filter2"),
          testUri
        )
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))
      when(locationService.list(Sequencer)).thenReturn(Future.successful(List(tcsLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceDsl.resolveByComponentNameAndType("TCS@obsMode", Sequencer).awaitResult
      actualLocations should ===(None)
    }
  }

  "resolveSequencer" must {

    "return a location which matches a given sequencerId and observing mode | ESW-119" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(
          AkkaConnection(ComponentId("TCS@obsMode1", Sequencer)),
          Prefix("tcs.test.filter1"),
          testUri
        )
      val ocsLocations = List(
        AkkaLocation(
          AkkaConnection(ComponentId("OCS_1", SequenceComponent)),
          Prefix("esw.test.filter"),
          testUri
        ),
        AkkaLocation(
          AkkaConnection(ComponentId("TCS_1", SequenceComponent)),
          Prefix("tcs.test.filter2"),
          testUri
        )
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceDsl.findSequencer("TCS", "obsMode1").futureValue
      actualLocations should ===(tcsLocation)
    }

    "return an IllegalArgumentException when no matching sequencerId and observing mode is found | ESW-119" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(
          AkkaConnection(ComponentId("TCS@obsMode1", Sequencer)),
          Prefix("tcs.test.filter1"),
          testUri
        )
      val ocsLocations = List(
        AkkaLocation(
          AkkaConnection(ComponentId("OCS_1", SequenceComponent)),
          Prefix("esw.test.filter"),
          testUri
        ),
        AkkaLocation(
          AkkaConnection(ComponentId("TCS_1", SequenceComponent)),
          Prefix("tcs.test.filter2"),
          testUri
        )
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      intercept[IllegalArgumentException] {
        locationServiceDsl.findSequencer("TCS", "obsMode2").awaitResult
      }
    }
  }

}
