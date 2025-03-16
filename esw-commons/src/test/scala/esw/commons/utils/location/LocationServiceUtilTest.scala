package esw.commons.utils.location

import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.CoordinatedShutdown.UnknownReason
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.exceptions.{
  OtherLocationIsRegistered,
  RegistrationFailed,
  RegistrationListingFailed => CswRegistrationListingFailed
}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.*
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models.{PekkoLocation, ComponentId, HttpLocation, Metadata}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.Subsystem.*
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.testcommons.BaseTestSuite

import java.net.URI
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.{reset, verify, when}
class LocationServiceUtilTest extends BaseTestSuite {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  private val locationService                                     = mock[LocationService]
  implicit val ec: ExecutionContext                               = system.executionContext

  // this only for creating TestProbe as the PekkoRegistration needs remote enabled actor Ref.
  private val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")

  private val subsystem       = TCS
  private val obsMode         = "DarkNight"
  private val prefix          = Prefix(subsystem, obsMode)
  private val actorRef        = TestProbe[Any]()(actorSystem).ref
  private val pekkoConnection = PekkoConnection(ComponentId(prefix, Sequencer))
  private val pekkoLocation   = PekkoLocation(pekkoConnection, actorRef.toURI, Metadata.empty)
  private val registration    = PekkoRegistrationFactory.make(pekkoConnection, actorRef)

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
      implicit val system: ActorSystem[?] = ActorSystem(Behaviors.empty, "test")
      val coordinatedShutdown             = CoordinatedShutdown(system)
      val registrationResult              = mock[RegistrationResult]
      when(registrationResult.location).thenReturn(pekkoLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(registration)).thenReturn(Future(registrationResult))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      locationServiceUtil.register(registration).rightValue should ===(pekkoLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service [OtherLocationIsRegistered] registration failure to error | ESW-214" in {
      implicit val system: ActorSystem[?] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      locationServiceUtil.register(registration).leftValue shouldBe EswLocationError.OtherLocationIsRegistered(errorMsg)
      system.terminate()
    }

    "map location service [RegistrationFailed] registration failure to error | ESW-214" in {
      implicit val system: ActorSystem[?] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(RegistrationFailed(errorMsg)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      locationServiceUtil.register(registration).leftValue shouldBe EswLocationError.RegistrationFailed(errorMsg)
      system.terminate()
    }
  }

  "listPekkoLocationsBy componentType" must {
    "list all pekko locations which match given componentType | ESW-324" in {
      val testUri = new URI("test-uri")
      val pekkoLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      val httpLocation =
        HttpLocation(HttpConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty)
      val sequenceComponentLocations = httpLocation :: pekkoLocations
      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      val actualLocations     = locationServiceUtil.listPekkoLocationsBy(SequenceComponent).rightValue

      actualLocations should ===(pekkoLocations)
    }

    "return empty list if no matching component type and filter is found | ESW-324" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceUtil.listPekkoLocationsBy(SequenceComponent, l => l.prefix.subsystem == WFOS).rightValue

      actualLocations should ===(List.empty)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-324" in {
      when(locationService.list(SequenceComponent)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listPekkoLocationsBy(SequenceComponent).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "listPekkoLocationsBy subsystem and componentType" must {
    "list all locations which match given componentType and subsystem | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_3"), SequenceComponent)), testUri, Metadata.empty)
      )
      val sequenceComponentLocations = tcsLocations ++ List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(Subsystem.OSS, "OSS_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listPekkoLocationsBy(TCS, SequenceComponent).rightValue

      actualLocations should ===(tcsLocations)
    }

    "return empty list if no matching component type and subsystem is found | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listPekkoLocationsBy(NFIRAOS, SequenceComponent).rightValue

      actualLocations should ===(List.empty)
    }

    "return a RegistrationListingFailed when location service call throws exception  | ESW-144, ESW-215" in {
      when(locationService.list(SequenceComponent)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listPekkoLocationsBy(subsystem, SequenceComponent).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "listSequencersPekkoLocationBy obsMode" must {
    "list all sequencers locations which match given obsMode" in {
      val testUri = new URI("test-uri")

      val darkNightSequencerLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "DarkNight.variation1"), Sequencer)), testUri, Metadata.empty)
      )

      val sequencerLocations =
        PekkoLocation(
          PekkoConnection(ComponentId(Prefix(IRIS, "ClearSkies.variation2"), Sequencer)),
          testUri,
          Metadata.empty
        ) :: darkNightSequencerLocations

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listSequencersPekkoLocationsBy("DarkNight").rightValue

      actualLocations should ===(darkNightSequencerLocations)
    }

    "return empty list if no matching component type and componentName is found" in {
      val testUri = new URI("test-uri")

      val sequencerLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "ClearSkies"), Sequencer)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "ClearSkies"), Sequencer)), testUri, Metadata.empty)
      )

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listSequencersPekkoLocationsBy("DarkNight").rightValue

      actualLocations should ===(List.empty[PekkoLocation])
    }

    "return a RegistrationListingFailed when location service call throws exception" in {
      when(locationService.list(Sequencer)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listSequencersPekkoLocationsBy("DarkNight").leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "listPekkoLocationsBy componentName and componentType" must {
    "list all locations which match given componentType and componentName" in {
      val testUri = new URI("test-uri")

      val darkNightSequencerLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "DarkNight"), Sequencer)), testUri, Metadata.empty)
      )

      val sequencerLocations =
        PekkoLocation(
          PekkoConnection(ComponentId(Prefix(IRIS, "ClearSkies"), Sequencer)),
          testUri,
          Metadata.empty
        ) :: darkNightSequencerLocations

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listPekkoLocationsBy("DarkNight", Sequencer).rightValue

      actualLocations should ===(darkNightSequencerLocations)
    }

    "return empty list if no matching component type and componentName is found" in {
      val testUri = new URI("test-uri")

      val sequencerLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "ClearSkies"), Sequencer)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "ClearSkies"), Sequencer)), testUri, Metadata.empty)
      )

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listPekkoLocationsBy("DarkNight", Sequencer).rightValue

      actualLocations should ===(List.empty[PekkoLocation])
    }

    "return a RegistrationListingFailed when location service call throws exception" in {
      when(locationService.list(Sequencer)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listPekkoLocationsBy("DarkNight", Sequencer).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "findByComponentNameAndType" must {
    "return a location which matches a given component name and type | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty)
      val ocsLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "ocs_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "tcs_1"), SequenceComponent)), testUri, Metadata.empty)
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))
      when(locationService.list(Sequencer)).thenReturn(Future.successful(List(tcsLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      val actualLocations =
        locationServiceUtil.findByComponentNameAndType("DarkNight", Sequencer).rightValue
      actualLocations should ===(tcsLocation)
    }

    "return an LocationNotFound when no matching component name and type is found | ESW-215" in {
      val testUri     = new URI("test-uri")
      val tcsLocation = PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty)
      val ocsLocations = List(
        PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "OCS_1"), SequenceComponent)), testUri, Metadata.empty),
        PekkoLocation(PekkoConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty)
      )
      when(locationService.list).thenReturn(Future.successful(tcsLocation :: ocsLocations))
      when(locationService.list(Sequencer)).thenReturn(Future.successful(List(tcsLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      val error =
        locationServiceUtil.findByComponentNameAndType("obsMode", Sequencer).leftValue

      error should ===(
        LocationNotFound(
          s"Could not find location matching ComponentName: obsMode, componentType: $Sequencer"
        )
      )
    }

    "return an RegistrationListingFailed when location service throws exception | ESW-215" in {
      when(locationService.list(Sequencer)).thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      val error               = locationServiceUtil.findByComponentNameAndType(obsMode, Sequencer).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "resolve" must {
    "return a location which matches a given connection | ESW-119" in {
      when(locationService.resolve(pekkoConnection, 10.millis))
        .thenReturn(Future.successful(Some(pekkoLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceUtil.resolve(pekkoConnection, 10.millis).rightValue

      actualLocations should ===(pekkoLocation)
    }

    "return a LocationNotFound when no matching connection is found | ESW-119" in {
      when(locationService.resolve(pekkoConnection, 200.millis))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      LocationNotFound(s"Could not resolve location matching connection: $pekkoConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.resolve(pekkoConnection, 200.millis))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "find" must {
    "return a location which matches a given connection | ESW-176" in {
      when(locationService.find(pekkoConnection))
        .thenReturn(Future.successful(Some(pekkoLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceUtil.find(pekkoConnection).rightValue

      actualLocations should ===(pekkoLocation)
      verify(locationService).find(pekkoConnection)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-176" in {
      when(locationService.find(pekkoConnection))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.find(pekkoConnection).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }

    "return a LocationNotFound when location find call returns None | ESW-176" in {
      when(locationService.find(pekkoConnection))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.find(pekkoConnection).leftValue shouldBe
      LocationNotFound(s"Could not find location matching connection: $pekkoConnection")
    }
  }

  "resolveSequencer" must {
    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationService.resolve(pekkoConnection, within = 10.millis))
        .thenReturn(Future.successful(Some(pekkoLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, within = 10.millis).rightValue shouldBe pekkoLocation

      verify(locationService).resolve(pekkoConnection, 10.millis)
    }

    "return a LocationNotFound when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationService.resolve(pekkoConnection, 200.millis))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      LocationNotFound(s"Could not resolve location matching connection: $pekkoConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.resolve(pekkoConnection, 200.millis))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "findSequencer" must {
    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationService.find(pekkoConnection))
        .thenReturn(Future.successful(Some(pekkoLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findSequencer(pekkoConnection.prefix).rightValue shouldBe pekkoLocation

      verify(locationService).find(pekkoConnection)
    }

    "return a LocationNotFound when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationService.find(pekkoConnection))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findSequencer(pekkoConnection.prefix).leftValue should ===(
        LocationNotFound(s"Could not find location matching connection: $pekkoConnection")
      )

      verify(locationService).find(pekkoConnection)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.find(pekkoConnection))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findSequencer(pekkoConnection.prefix).leftValue should ===(
        RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
      )

      verify(locationService).find(pekkoConnection)
    }
  }

  "findAgentByHostname" must {
    val hostname = "192.168.1.3"
    val uri      = new URI(s"http://$hostname:76543/iris")

    "return a machine location which matches the given hostname | ESW-480" in {
      val location1 = PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "ocs_1"), Machine)), uri, Metadata.empty)
      val location2 = PekkoLocation(PekkoConnection(ComponentId(Prefix(ESW, "ocs_2"), Machine)), uri, Metadata.empty)

      when(locationService.list(hostname)).thenReturn(Future.successful(List(location1, location2)))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findAgentByHostname(hostname).rightValue should ===(location1)
      verify(locationService).list(hostname)
    }

    "return a LocationNotFound when no matching host is found | ESW-480" in {
      when(locationService.list(hostname)).thenReturn(Future.successful(List()))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findAgentByHostname(hostname).leftValue should ===(
        LocationNotFound(s"No agent running on host: $hostname")
      )

      verify(locationService).list(hostname)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-480" in {
      when(locationService.list(hostname)).thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findAgentByHostname(hostname).leftValue should ===(
        RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
      )

      verify(locationService).list(hostname)
    }
  }
}
