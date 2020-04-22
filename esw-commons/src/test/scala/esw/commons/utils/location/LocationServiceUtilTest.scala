package esw.commons.utils.location

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, AkkaRegistration, ComponentId}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.commons.{BaseTestSuite, Timeouts}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class LocationServiceUtilTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val locationService       = mock[LocationService]
  implicit val ec: ExecutionContext = system.executionContext

  private val prefix         = Prefix("tcs.home.datum")
  private val uri            = new URI("uri")
  private val akkaConnection = AkkaConnection(ComponentId(prefix, Sequencer))
  private val registration   = AkkaRegistration(akkaConnection, uri)
  private val akkaLocation   = AkkaLocation(akkaConnection, uri)

  "register" must {
    "return successful RegistrationResult | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val coordinatedShutdown             = CoordinatedShutdown(system.toClassic)
      val registrationResult              = mock[RegistrationResult]
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration)).thenReturn(Future(registrationResult))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val onFailure          = mock[PartialFunction[Throwable, Future[Either[Int, AkkaLocation]]]]

      locationServiceDsl.register(registration, onFailure).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service registration failure using the given failure mapper function | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      val onFailure: PartialFunction[Throwable, Future[Either[Int, AkkaLocation]]] = {
        case e: Throwable => Future.successful(Left(5))
      }

      locationServiceDsl.register(registration, onFailure).leftValue shouldBe 5
      system.terminate()
    }
  }

  "listBySubsystem" must {
    "list all locations which match given componentType and subsystem | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_3"), SequenceComponent)), testUri)
      )
      val sequenceComponentLocations = tcsLocations ++ List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(Subsystem.OSS, "OSS_1"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceDsl.listBy(TCS, SequenceComponent).rightValue

      actualLocations should ===(tcsLocations)
    }

    "return empty list if no matching component type and subsystem is found | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceDsl.listBy(Subsystem.NFIRAOS, SequenceComponent).rightValue

      actualLocations should ===(List.empty)
    }
  }

  "resolveByComponentNameAndType" must {
    "return a location which matches a given component name and type | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "obsmode1"), Sequencer)), testUri)
      val ocsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "ocs_1"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "tcs_1"), SequenceComponent)), testUri)
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))
      when(locationService.list(Sequencer)).thenReturn(Future.successful(List(tcsLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceDsl.resolveByComponentNameAndType("obsmode1", Sequencer).rightValue
      actualLocations should ===(tcsLocation)
    }

    "return an IllegalArgumentException when no matching component name and type is found | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(TCS, "obsMode1"), Sequencer)),
          testUri
        )
      val ocsLocations = List(
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(ESW, "OCS_1"), SequenceComponent)),
          testUri
        ),
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)),
          testUri
        )
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))
      when(locationService.list(Sequencer)).thenReturn(Future.successful(List(tcsLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceDsl.resolveByComponentNameAndType("obsMode", Sequencer).awaitResult

      actualLocations.leftValue should ===(
        ResolveLocationFailed(
          s"Could not find location matching ComponentName: obsMode, componentType: $Sequencer"
        )
      )
    }
  }

  "resolveSequencer" must {

    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(TCS, "obsMode1"), Sequencer)),
          testUri
        )

      when(locationService.resolve(AkkaConnection(ComponentId(Prefix(TCS, "obsMode1"), Sequencer)), Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Some(tcsLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceDsl.resolveSequencer(TCS, "obsMode1").rightValue
      actualLocations should ===(tcsLocation)
    }

    "return a RuntimeException when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationService.resolve(AkkaConnection(ComponentId(Prefix(TCS, "obsMode1"), Sequencer)), Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      intercept[RuntimeException] {
        locationServiceUtil.resolveSequencer(TCS, "obsMode2", 200.millis).awaitResult
      }
    }
  }

}
