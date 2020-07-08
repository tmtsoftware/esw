package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok}
import esw.sm.api.protocol.AgentError.SpawnSequenceComponentFailed
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ShutdownSequenceComponentsPolicy.{AllSequenceComponents, SingleSequenceComponent}
import esw.sm.api.protocol.{ShutdownSequenceComponentResponse, SpawnSequenceComponentResponse}
import esw.testcommons.BaseTestSuite

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequenceComponentUtilTest extends BaseTestSuite {
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  private val locationServiceUtil = mock[LocationServiceUtil]
  private val agentUtil           = mock[AgentUtil]
  private val sequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
    override private[sm] def idleSequenceComponent(
        sequenceComponentLocation: AkkaLocation
    ): Future[Option[SequenceComponentApi]] =
      sequenceComponentLocation.prefix.subsystem match {
        case TCS => Future.successful(None)
        case _   => Future.successful(Some(mock[SequenceComponentApi]))
      }

  }

  private def mockAkkaLocation(prefixStr: String) =
    AkkaLocation(AkkaConnection(ComponentId(Prefix(prefixStr), SequenceComponent)), new URI("some-uri"))
  private val tcsLocations = futureRight(List(mockAkkaLocation("TCS.primary"), mockAkkaLocation("TCS.secondary")))
  private val eswLocations = futureRight(List(mockAkkaLocation("ESW.primary")))

  override def beforeEach(): Unit = reset(locationServiceUtil, agentUtil)

  override def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }

  "spawnSequenceComponent" must {
    "spawn new sequence component for given name and agent prefix | ESW-337" in {
      val seqCompName                                  = "seq_comp"
      val agent                                        = Prefix(TCS, "tcs.primary")
      val seqComp                                      = ComponentId(Prefix(agent.subsystem, seqCompName), SequenceComponent)
      val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)

      val sequenceComponentApi = mock[SequenceComponentImpl]
      when(agentUtil.spawnSequenceComponentFor(agent, seqCompName))
        .thenReturn(futureRight(sequenceComponentApi))

      sequenceComponentUtil.spawnSequenceComponent(agent, seqCompName).futureValue should ===(
        SpawnSequenceComponentResponse.Success(seqComp)
      )

      verify(agentUtil).spawnSequenceComponentFor(agent, seqCompName)
    }

    "return failure if agent fails to spawn sequence component | ESW-337" in {
      val seqCompName                                  = "seq_comp"
      val agent                                        = Prefix(TCS, "tcs.primary")
      val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)

      when(agentUtil.spawnSequenceComponentFor(agent, seqCompName))
        .thenReturn(futureLeft(SpawnSequenceComponentFailed("spawn failed")))

      sequenceComponentUtil.spawnSequenceComponent(agent, seqCompName).futureValue should ===(
        SpawnSequenceComponentFailed("spawn failed")
      )

      verify(agentUtil).spawnSequenceComponentFor(agent, seqCompName)
    }
  }

  "getAvailableSequenceComponent" must {
    "return available sequence component for given subsystem | ESW-164" in {
      val irisLocations = futureRight(List(mockAkkaLocation("IRIS.primary"), mockAkkaLocation("IRIS.secondary")))
      when(locationServiceUtil.listAkkaLocationsBy(IRIS, SequenceComponent)).thenReturn(irisLocations)

      sequenceComponentUtil.getAvailableSequenceComponent(IRIS).rightValue shouldBe a[SequenceComponentApi]

      // verify call for looking iris sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(IRIS, SequenceComponent)
      // verify that agent.spawnSequenceComponentFor call is NOT made
      verify(agentUtil, never).spawnSequenceComponentFor(ESW)
    }

    "return available ESW sequence component when specific subsystem sequence component is not available | ESW-164" in {
      when(locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent)).thenReturn(tcsLocations)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, SequenceComponent)).thenReturn(eswLocations)

      sequenceComponentUtil.getAvailableSequenceComponent(TCS).rightValue shouldBe a[SequenceComponentApi]

      // verify call for looking tcs sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(TCS, SequenceComponent)

      // verify call for looking esw sequence components as tcs sequence components are not idle/available
      // stub for idleSequenceComponent(tcs) returns None to mimic tcs sequence components NOT idle situation
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, SequenceComponent)

      // esw seq comp is available so no need to spawn seq comp using agent.
      // verify agent.spawnSequenceComponentFor call is NOT made
      verify(agentUtil, never).spawnSequenceComponentFor(ESW)
    }

    "spawn new sequence component when subsystem and esw both sequence components are not available | ESW-164" in {
      val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def idleSequenceComponent(
            sequenceComponentLocation: AkkaLocation
        ): Future[Option[SequenceComponentApi]] =
          sequenceComponentLocation.prefix.subsystem match {
            case _ => Future.successful(None) // stub this mimic no sequence component is idle
          }
      }

      when(locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent)).thenReturn(tcsLocations)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, SequenceComponent)).thenReturn(eswLocations)

      val sequenceComponentApi = mock[SequenceComponentApi]
      when(agentUtil.spawnSequenceComponentFor(ESW)).thenReturn(futureRight(sequenceComponentApi))

      sequenceComponentUtil.getAvailableSequenceComponent(TCS).rightValue should ===(sequenceComponentApi)

      // verify call for looking tcs sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(TCS, SequenceComponent)
      // verify call for looking esw sequence components as tcs sequence components are not idle/available
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, SequenceComponent)
      // verify agent.spawnSequenceComponentFor call for tcs
      verify(agentUtil, times(1)).spawnSequenceComponentFor(ESW)
    }

    "return SpawnSequenceComponentFailed if spawning sequence component fails | ESW-164" in {
      val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def idleSequenceComponent(
            sequenceComponentLocation: AkkaLocation
        ): Future[Option[SequenceComponentApi]] =
          sequenceComponentLocation.prefix.subsystem match {
            case _ => Future.successful(None) // stub this mimic no sequence component is idle
          }
      }

      val spawnFailed = SpawnSequenceComponentFailed("Error in spawning sequence component")

      when(locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent)).thenReturn(tcsLocations)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, SequenceComponent)).thenReturn(eswLocations)
      when(agentUtil.spawnSequenceComponentFor(ESW)).thenReturn(futureLeft(spawnFailed))

      sequenceComponentUtil.getAvailableSequenceComponent(TCS).leftValue should ===(spawnFailed)

      // verify call for looking tcs sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(TCS, SequenceComponent)
      // verify call for looking esw sequence components as tcs sequence components are not idle/available
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, SequenceComponent)
      verify(agentUtil).spawnSequenceComponentFor(ESW)
    }
  }

  "idleSequenceComponent" must {
    "return none if sequence component is running a sequencer | ESW-164" in {
      val mockSeqCompImpl = mock[SequenceComponentImpl]
      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentImpl =
          mockSeqCompImpl
      }

      when(mockSeqCompImpl.status)
        .thenReturn(Future.successful(GetStatusResponse(Some(mockAkkaLocation("IRIS.darknight")))))

      seqCompUtil.idleSequenceComponent(mockAkkaLocation("ESW.backup")).futureValue should ===(None)
    }
  }

  "shutdown" must {
    "return success when shutdown of single sequence component is successful | ESW-338" in {
      val mockSeqCompImpl = mock[SequenceComponentImpl]
      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentImpl =
          mockSeqCompImpl
      }
      val prefixStr      = "ESW.primary"
      val akkaConnection = AkkaConnection(ComponentId(Prefix(prefixStr), SequenceComponent))
      when(locationServiceUtil.find(akkaConnection))
        .thenReturn(Future.successful(Right(mockAkkaLocation(prefixStr))))
      when(mockSeqCompImpl.shutdown()).thenReturn(Future.successful(Ok))

      val singleShutdownPolicy = SingleSequenceComponent(Prefix(prefixStr))
      seqCompUtil.shutdown(singleShutdownPolicy).futureValue should ===(ShutdownSequenceComponentResponse.Success)

      verify(locationServiceUtil).find(akkaConnection)
      verify(mockSeqCompImpl).shutdown()
    }

    "return error when location service returns error while shutting down single sequencer | ESW-338" in {
      val mockSeqCompImpl = mock[SequenceComponentImpl]
      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentImpl =
          mockSeqCompImpl
      }
      val prefixStr  = "ESW.primary"
      val connection = AkkaConnection(ComponentId(Prefix(prefixStr), SequenceComponent))
      when(locationServiceUtil.find(connection))
        .thenReturn(Future.successful(Left(LocationNotFound("error"))))

      val singleShutdownPolicy = SingleSequenceComponent(Prefix(prefixStr))
      seqCompUtil.shutdown(singleShutdownPolicy).futureValue should ===(LocationServiceError("error"))

      verify(locationServiceUtil).find(connection)
      verify(mockSeqCompImpl, never).shutdown()
    }

    "return success when shutting down all sequence components is successful | ESW-346" in {

      val eswSeqCompLoc   = mockAkkaLocation("ESW.primary")
      val irisSeqCompLoc  = mockAkkaLocation("IRIS.primary")
      val eswSeqCompImpl  = mock[SequenceComponentImpl]
      val irisSeqCompImpl = mock[SequenceComponentImpl]

      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(loc: AkkaLocation): SequenceComponentImpl =
          if (loc.prefix.subsystem == ESW) eswSeqCompImpl else irisSeqCompImpl
      }

      when(locationServiceUtil.listAkkaLocationsBy(SequenceComponent))
        .thenReturn(Future.successful(Right(List(eswSeqCompLoc, irisSeqCompLoc))))

      when(eswSeqCompImpl.shutdown()).thenReturn(Future.successful(Ok))
      when(irisSeqCompImpl.shutdown()).thenReturn(Future.successful(Ok))

      seqCompUtil.shutdown(AllSequenceComponents).futureValue should ===(ShutdownSequenceComponentResponse.Success)

      verify(locationServiceUtil).listAkkaLocationsBy(SequenceComponent)
      verify(eswSeqCompImpl).shutdown()
      verify(irisSeqCompImpl).shutdown()
    }

    "return error when location service returns error while shutting down all sequence components | ESW-346" in {
      val mockSeqCompImpl = mock[SequenceComponentImpl]
      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentImpl =
          mockSeqCompImpl
      }
      when(locationServiceUtil.listAkkaLocationsBy(SequenceComponent))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("error"))))

      seqCompUtil.shutdown(AllSequenceComponents).futureValue should ===(LocationServiceError("error"))

      verify(locationServiceUtil).listAkkaLocationsBy(SequenceComponent)
      verify(mockSeqCompImpl, never).shutdown()
    }
  }
}
