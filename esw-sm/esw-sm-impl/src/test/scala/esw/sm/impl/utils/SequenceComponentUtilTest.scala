package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.commons.utils.location.EswLocationError.LocationNotFound
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.models.SequenceComponentState
import esw.ocs.api.protocol.SequenceComponentResponse
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok}
import esw.sm.api.protocol.AgentError.SpawnSequenceComponentFailed
import esw.sm.api.protocol.CommonFailure.LocationServiceError
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
    "spawn new sequence component for given componentId and with given name | ESW-337" in {
      val seqCompName                                  = "seq_comp"
      val componentId                                  = ComponentId(Prefix(TCS, "tcs.primary"), Machine)
      val seqCompPrefix                                = Prefix(componentId.prefix.subsystem, seqCompName)
      val seqComp                                      = ComponentId(seqCompPrefix, SequenceComponent)
      val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)

      val sequenceComponentApi = mock[SequenceComponentImpl]
      when(agentUtil.spawnSequenceComponentFor(seqCompPrefix)).thenReturn(futureRight(sequenceComponentApi))

      sequenceComponentUtil.spawnSequenceComponent(componentId, seqCompName).futureValue should ===(
        SpawnSequenceComponentResponse.Success(seqComp)
      )

      verify(agentUtil).spawnSequenceComponentFor(seqCompPrefix)
    }

    "return failure if agent fails to spawn sequence component | ESW-337" in {
      val componentId                                  = ComponentId(Prefix(TCS, "tcs.primary"), Machine)
      val seqCompName                                  = "seq_comp"
      val seqCompPrefix                                = Prefix(componentId.prefix.subsystem, seqCompName)
      val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)

      when(agentUtil.spawnSequenceComponentFor(seqCompPrefix))
        .thenReturn(futureLeft(SpawnSequenceComponentFailed("spawn failed")))

      sequenceComponentUtil.spawnSequenceComponent(componentId, seqCompName).futureValue should ===(
        SpawnSequenceComponentFailed("spawn failed")
      )

      verify(agentUtil).spawnSequenceComponentFor(seqCompPrefix)
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

  "shutdown by prefix" must {

    "return success when shutdown sequence component is successful | ESW-338" in {
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

      seqCompUtil.shutdown(Prefix(prefixStr)).futureValue should ===(ShutdownSequenceComponentResponse.Success)

      verify(locationServiceUtil).find(akkaConnection)
      verify(mockSeqCompImpl).shutdown()
    }

    "return error when unload script returns error | ESW-338" in {
      val mockSeqCompImpl = mock[SequenceComponentImpl]
      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentImpl =
          mockSeqCompImpl
      }
      val prefixStr  = "ESW.primary"
      val connection = AkkaConnection(ComponentId(Prefix(prefixStr), SequenceComponent))
      when(locationServiceUtil.find(connection))
        .thenReturn(Future.successful(Right(mockAkkaLocation(prefixStr))))
      when(mockSeqCompImpl.shutdown())
        .thenReturn(Future.successful(SequenceComponentResponse.Unhandled(SequenceComponentState.Idle, "UnloadScript")))

      seqCompUtil.shutdown(Prefix(prefixStr)).futureValue should ===(
        ShutdownSequenceComponentResponse.ShutdownSequenceComponentFailure(
          Prefix(prefixStr),
          s"Sequence Component can not accept 'UnloadScript' message in '${SequenceComponentState.Idle.entryName}'"
        )
      )

      verify(locationServiceUtil).find(connection)
      verify(mockSeqCompImpl).shutdown()
    }

    "return error when location service returns error | ESW-338" in {
      val mockSeqCompImpl = mock[SequenceComponentImpl]
      val seqCompUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
        override private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentImpl =
          mockSeqCompImpl
      }
      val prefixStr  = "ESW.primary"
      val connection = AkkaConnection(ComponentId(Prefix(prefixStr), SequenceComponent))
      when(locationServiceUtil.find(connection))
        .thenReturn(Future.successful(Left(LocationNotFound("error"))))

      seqCompUtil.shutdown(Prefix(prefixStr)).futureValue should ===(LocationServiceError("error"))
      verify(locationServiceUtil).find(connection)
      verify(mockSeqCompImpl, never).shutdown()
    }
  }
}
