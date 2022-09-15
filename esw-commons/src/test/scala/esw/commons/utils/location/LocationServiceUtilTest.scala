/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.location

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
import csw.location.api.models.ComponentType.*
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Metadata}
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

  // this only for creating TestProbe as the AkkaRegistration needs remote enabled actor Ref.
  private val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")

  private val subsystem      = TCS
  private val obsMode        = "DarkNight"
  private val prefix         = Prefix(subsystem, obsMode)
  private val actorRef       = TestProbe[Any]()(actorSystem).ref
  private val akkaConnection = AkkaConnection(ComponentId(prefix, Sequencer))
  private val akkaLocation   = AkkaLocation(akkaConnection, actorRef.toURI, Metadata.empty)
  private val registration   = AkkaRegistrationFactory.make(akkaConnection, actorRef)

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

      val locationServiceUtil = new LocationServiceUtil(locationService)

      locationServiceUtil.register(registration).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service [OtherLocationIsRegistered] registration failure to error | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      locationServiceUtil.register(registration).leftValue shouldBe EswLocationError.OtherLocationIsRegistered(errorMsg)
      system.terminate()
    }

    "map location service [RegistrationFailed] registration failure to error | ESW-214" in {
      implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
      val errorMsg                        = "error message"
      when(locationService.register(registration)).thenReturn(Future.failed(RegistrationFailed(errorMsg)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      locationServiceUtil.register(registration).leftValue shouldBe EswLocationError.RegistrationFailed(errorMsg)
      system.terminate()
    }
  }

  "listAkkaLocationsBy componentType" must {
    "list all akka locations which match given componentType | ESW-324" in {
      val testUri = new URI("test-uri")
      val akkaLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      val httpLocation =
        HttpLocation(HttpConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty)
      val sequenceComponentLocations = httpLocation :: akkaLocations
      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      val actualLocations     = locationServiceUtil.listAkkaLocationsBy(SequenceComponent).rightValue

      actualLocations should ===(akkaLocations)
    }

    "return empty list if no matching component type and filter is found | ESW-324" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listAkkaLocationsBy(SequenceComponent, l => l.prefix.subsystem == WFOS).rightValue

      actualLocations should ===(List.empty)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-324" in {
      when(locationService.list(SequenceComponent)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listAkkaLocationsBy(SequenceComponent).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "listAkkaLocationsBy subsystem and componentType" must {
    "list all locations which match given componentType and subsystem | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_3"), SequenceComponent)), testUri, Metadata.empty)
      )
      val sequenceComponentLocations = tcsLocations ++ List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(Subsystem.OSS, "OSS_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent).rightValue

      actualLocations should ===(tcsLocations)
    }

    "return empty list if no matching component type and subsystem is found | ESW-144, ESW-215" in {
      val testUri = new URI("test-uri")
      val sequenceComponentLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_2"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "IRIS_1"), SequenceComponent)), testUri, Metadata.empty)
      )

      when(locationService.list(SequenceComponent)).thenReturn(Future.successful(sequenceComponentLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listAkkaLocationsBy(NFIRAOS, SequenceComponent).rightValue

      actualLocations should ===(List.empty)
    }

    "return a RegistrationListingFailed when location service call throws exception  | ESW-144, ESW-215" in {
      when(locationService.list(SequenceComponent)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listAkkaLocationsBy(subsystem, SequenceComponent).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "listSequencersAkkaLocationBy obsMode" must {
    "list all sequencers locations which match given obsMode" in {
      val testUri = new URI("test-uri")

      val darkNightSequencerLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "DarkNight.variation1"), Sequencer)), testUri, Metadata.empty)
      )

      val sequencerLocations =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(IRIS, "ClearSkies.variation2"), Sequencer)),
          testUri,
          Metadata.empty
        ) :: darkNightSequencerLocations

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listSequencersAkkaLocationsBy("DarkNight").rightValue

      actualLocations should ===(darkNightSequencerLocations)
    }

    "return empty list if no matching component type and componentName is found" in {
      val testUri = new URI("test-uri")

      val sequencerLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "ClearSkies"), Sequencer)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "ClearSkies"), Sequencer)), testUri, Metadata.empty)
      )

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listSequencersAkkaLocationsBy("DarkNight").rightValue

      actualLocations should ===(List.empty[AkkaLocation])
    }

    "return a RegistrationListingFailed when location service call throws exception" in {
      when(locationService.list(Sequencer)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listSequencersAkkaLocationsBy("DarkNight").leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "listAkkaLocationsBy componentName and componentType" must {
    "list all locations which match given componentType and componentName" in {
      val testUri = new URI("test-uri")

      val darkNightSequencerLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "DarkNight"), Sequencer)), testUri, Metadata.empty)
      )

      val sequencerLocations =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(IRIS, "ClearSkies"), Sequencer)),
          testUri,
          Metadata.empty
        ) :: darkNightSequencerLocations

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listAkkaLocationsBy("DarkNight", Sequencer).rightValue

      actualLocations should ===(darkNightSequencerLocations)
    }

    "return empty list if no matching component type and componentName is found" in {
      val testUri = new URI("test-uri")

      val sequencerLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "ClearSkies"), Sequencer)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "ClearSkies"), Sequencer)), testUri, Metadata.empty)
      )

      when(locationService.list(Sequencer)).thenReturn(Future.successful(sequencerLocations))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations = locationServiceUtil.listAkkaLocationsBy("DarkNight", Sequencer).rightValue

      actualLocations should ===(List.empty[AkkaLocation])
    }

    "return a RegistrationListingFailed when location service call throws exception" in {
      when(locationService.list(Sequencer)).thenReturn(Future.failed(cswRegistrationListingFailed))
      val locationServiceUtil = new LocationServiceUtil(locationService)

      val error = locationServiceUtil.listAkkaLocationsBy("DarkNight", Sequencer).leftValue

      error shouldBe RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "findByComponentNameAndType" must {
    "return a location which matches a given component name and type | ESW-215" in {
      val testUri = new URI("test-uri")
      val tcsLocation =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty)
      val ocsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "ocs_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "tcs_1"), SequenceComponent)), testUri, Metadata.empty)
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
      val tcsLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "DarkNight"), Sequencer)), testUri, Metadata.empty)
      val ocsLocations = List(
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "OCS_1"), SequenceComponent)), testUri, Metadata.empty),
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, "TCS_1"), SequenceComponent)), testUri, Metadata.empty)
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
      when(locationService.resolve(akkaConnection, 10.millis))
        .thenReturn(Future.successful(Some(akkaLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceUtil.resolve(akkaConnection, 10.millis).rightValue

      actualLocations should ===(akkaLocation)
    }

    "return a LocationNotFound when no matching connection is found | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      LocationNotFound(s"Could not resolve location matching connection: $akkaConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "find" must {
    "return a location which matches a given connection | ESW-176" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.successful(Some(akkaLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)

      val actualLocations =
        locationServiceUtil.find(akkaConnection).rightValue

      actualLocations should ===(akkaLocation)
      verify(locationService).find(akkaConnection)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-176" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.find(akkaConnection).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
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

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, within = 10.millis).rightValue shouldBe akkaLocation

      verify(locationService).resolve(akkaConnection, 10.millis)
    }

    "return a LocationNotFound when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      LocationNotFound(s"Could not resolve location matching connection: $akkaConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.resolveSequencer(prefix, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
    }
  }

  "findSequencer" must {
    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.successful(Some(akkaLocation)))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findSequencer(akkaConnection.prefix).rightValue shouldBe akkaLocation

      verify(locationService).find(akkaConnection)
    }

    "return a LocationNotFound when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.successful(None))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findSequencer(akkaConnection.prefix).leftValue should ===(
        LocationNotFound(s"Could not find location matching connection: $akkaConnection")
      )

      verify(locationService).find(akkaConnection)
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationService.find(akkaConnection))
        .thenReturn(Future.failed(cswRegistrationListingFailed))

      val locationServiceUtil = new LocationServiceUtil(locationService)
      locationServiceUtil.findSequencer(akkaConnection.prefix).leftValue should ===(
        RegistrationListingFailed(s"Location Service Error: $cswLocationServiceErrorMsg")
      )

      verify(locationService).find(akkaConnection)
    }
  }

  "findAgentByHostname" must {
    val hostname = "192.168.1.3"
    val uri      = new URI(s"http://$hostname:76543/iris")

    "return a machine location which matches the given hostname | ESW-480" in {
      val location1 = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "ocs_1"), Machine)), uri, Metadata.empty)
      val location2 = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "ocs_2"), Machine)), uri, Metadata.empty)

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
