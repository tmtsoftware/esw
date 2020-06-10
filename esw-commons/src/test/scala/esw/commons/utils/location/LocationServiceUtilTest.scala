package esw.commons.utils.location

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.exceptions.{
  OtherLocationIsRegistered,
  RegistrationFailed,
  RegistrationListingFailed => CswRegistrationListingFailed
}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.BaseTestSuite
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class LocationServiceUtilTest extends BaseTestSuite {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  private val locationService                                     = mock[LocationService]
  implicit val ec: ExecutionContext                               = system.executionContext

  // this only for creating TestProbe as the AkkaRegistration needs remote enabled actor Ref.
  private val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")

  private val subsystem      = TCS
  private val observingMode  = "darknight"
  private val prefix         = Prefix(subsystem, observingMode)
  private val uri            = TestProbe[Any]()(actorSystem).ref.toURI
  private val akkaConnection = AkkaConnection(ComponentId(prefix, Sequencer))
  private val akkaLocation   = AkkaLocation(akkaConnection, uri)
  private val registration   = AkkaRegistrationFactory.make(akkaConnection, uri)

  private val cswRegistrationListingFailed: CswRegistrationListingFailed = CswRegistrationListingFailed()
  private val cswLocationServiceErrorMsg: String                         = cswRegistrationListingFailed.getMessage

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService)
  }

  protected override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  "register" must {
    "return successful RegistrationResult | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val coordinatedShutdown             = CoordinatedShutdown(system)
      val registrationResult              = mock[RegistrationResult]
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration)).thenReturn(Future(registrationResult))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      locationServiceDsl.register(registration).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service [OtherLocationIsRegistered] registration failure to error | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      locationServiceDsl.register(registration).leftValue shouldBe EswLocationError.OtherLocationIsRegistered(errorMsg)
      system.terminate()
    }

    "map location service [RegistrationFailed] registration failure to error | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(RegistrationFailed(errorMsg)))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      locationServiceDsl.register(registration).leftValue shouldBe EswLocationError.RegistrationFailed(errorMsg)
      system.terminate()
    }
  }

  "listAkkaLocationsBy" must {

    "list all akka locations which match given componentType | ESW-324" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_3"), SequenceComponent)), testUri)
      )

      val httpUri = new URI("http://localhost:5676")

      val tcsHttpLocations = List(HttpLocation(HttpConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), httpUri))

      val otherSeqComponentAkkaLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(Subsystem.OSS, "OSS_1"), SequenceComponent)), testUri),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri)
      )
      val sequenceComponentLocations = tcsLocations ++ otherSeqComponentAkkaLocations ++ tcsHttpLocations

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceDsl.listAkkaLocationsBy(SequenceComponent).rightValue

      actualLocations should ===(tcsLocations ++ otherSeqComponentAkkaLocations)
    }

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

      val actualLocations = locationServiceDsl.listAkkaLocationsBy(TCS, SequenceComponent).rightValue

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

      val actualLocations = locationServiceDsl.listAkkaLocationsBy(Subsystem.NFIRAOS, SequenceComponent).rightValue

      actualLocations should ===(List.empty)
    }

    "return a RegistrationListingFailed when location service call throws exception  | ESW-144, ESW-215" in {
      when(locationService.list(SequenceComponent)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val error = locationServiceDsl.listAkkaLocationsBy(subsystem, SequenceComponent).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }

    "return a RegistrationListingFailed when location service call throws any non fatal exception  | ESW-144, ESW-215" in {
      when(locationService.list(SequenceComponent)).thenReturn(Future.failed(new RuntimeException("Unknown error")))
      val locationServiceDsl = new LocationServiceUtil(locationService)

      val error = locationServiceDsl.listAkkaLocationsBy(subsystem, SequenceComponent).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: Unknown error")
    }
  }

  "findByComponentNameAndType" must {
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
        locationServiceDsl.findByComponentNameAndType("obsmode1", Sequencer).rightValue
      actualLocations should ===(tcsLocation)
    }

    "return an LocationNotFound when no matching component name and type is found | ESW-215" in {
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
      val error =
        locationServiceDsl.findByComponentNameAndType("obsMode", Sequencer).leftValue

      error should ===(
        LocationNotFound(
          s"Could not find location matching ComponentName: obsMode, componentType: $Sequencer"
        )
      )
    }

    "return an RegistrationListingFailed when location service throws exception | ESW-215" in {
      when(locationService.list(Sequencer)).thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      val error =
        locationServiceDsl.findByComponentNameAndType(observingMode, Sequencer).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "resolve" must {
    "return a location which matches a given connection | ESW-119" in {
      when(locationService.resolve(akkaConnection, 10.millis))
        .thenReturn(Future.successful(Some(akkaLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceDsl.resolve(akkaConnection, 10.millis).rightValue

      actualLocations should ===(akkaLocation)
    }

    "return a LocationNotFound when no matching connection is found | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(subsystem, observingMode, 200.millis).leftValue shouldBe
      LocationNotFound(s"Could not resolve location matching connection: $akkaConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(subsystem, observingMode, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: ${cswLocationServiceErrorMsg}")
    }
  }

  "find" must {
    "return a location which matches a given connection | ESW-176" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.successful(Some(akkaLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceDsl.find(akkaConnection).rightValue

      actualLocations should ===(akkaLocation)
      verify(locationService).find(akkaConnection)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-176" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.find(akkaConnection).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: ${cswLocationServiceErrorMsg}")
    }

    "return a LocationNotFound when location find call returns None | ESW-176" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.find(akkaConnection).leftValue shouldBe
      LocationNotFound(s"Could not find location matching connection: $akkaConnection")
    }
  }

  "resolveSequencer" must {
    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationService.resolve(akkaConnection, within = 10.millis))
        .thenReturn(Future.successful(Some(akkaLocation)))

      val locationServiceDsl = new LocationServiceUtil(locationService)
      locationServiceDsl.resolveSequencer(subsystem, observingMode, within = 10.millis).rightValue shouldBe akkaLocation

      verify(locationService).resolve(akkaConnection, 10.millis)
    }

    "return a LocationNotFound when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(subsystem, observingMode, 200.millis).leftValue shouldBe
      LocationNotFound(s"Could not resolve location matching connection: $akkaConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(subsystem, observingMode, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }
}
