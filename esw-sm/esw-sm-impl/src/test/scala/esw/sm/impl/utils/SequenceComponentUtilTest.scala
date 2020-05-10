package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.commons.BaseTestSuite
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.sm.api.models.SequenceManagerError.SpawnSequenceComponentFailed

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequenceComponentUtilTest extends BaseTestSuite {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")
  implicit val timeout: Timeout                                = 1.hour

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
  val agentUtil: AgentUtil                     = mock[AgentUtil]

  val sequenceComponentUtil: SequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil) {
    override private[sm] def idleSequenceComponent(
        sequenceComponentLocation: AkkaLocation
    ): Future[Option[SequenceComponentApi]] =
      sequenceComponentLocation.prefix.subsystem match {
        case TCS => Future.successful(None)
        case _   => Future.successful(Some(mock[SequenceComponentApi]))
      }

  }

  def mockAkkaLocation(prefixStr: String): AkkaLocation =
    AkkaLocation(AkkaConnection(ComponentId(Prefix(prefixStr), SequenceComponent)), new URI("some-uri"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationServiceUtil, agentUtil)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  "getAvailableSequenceComponent" must {
    "return available sequence component for given subsystem | ESW-164" in {
      when(locationServiceUtil.listAkkaLocationsBy(IRIS, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("IRIS.primary"), mockAkkaLocation("IRIS.secondary")))))

      sequenceComponentUtil.getAvailableSequenceComponent(IRIS).rightValue shouldBe a[SequenceComponentApi]

      // verify call for looking iris sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(IRIS, SequenceComponent)
      // verify that agent.spawnSequenceComponentFor call is NOT made
      verify(agentUtil, never).spawnSequenceComponentFor(IRIS)
    }

    "return available ESW sequence component when specific subsystem sequence component is not available | ESW-164" in {

      when(locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("TCS.primary"), mockAkkaLocation("TCS.secondary")))))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("ESW.primary")))))

      sequenceComponentUtil.getAvailableSequenceComponent(TCS).rightValue shouldBe a[SequenceComponentApi]

      // verify call for looking tcs sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(TCS, SequenceComponent)

      // verify call for looking esw sequence components as tcs sequence components are not idle/available
      // stub for idleSequenceComponent(tcs) returns None to mimic tcs sequence components NOT idle situation
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, SequenceComponent)

      // esw seq comp is available so no need to spawn seq comp using agent.
      // verify agent.spawnSequenceComponentFor call is NOT made
      verify(agentUtil, never).spawnSequenceComponentFor(TCS)
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

      when(locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("TCS.primary"), mockAkkaLocation("TCS.secondary")))))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("ESW.primary")))))

      when(agentUtil.spawnSequenceComponentFor(TCS)).thenReturn(Future.successful(Right(mock[SequenceComponentApi])))

      sequenceComponentUtil.getAvailableSequenceComponent(TCS).rightValue shouldBe a[SequenceComponentApi]

      // verify call for looking tcs sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(TCS, SequenceComponent)
      // verify call for looking esw sequence components as tcs sequence components are not idle/available
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, SequenceComponent)
      // verify agent.spawnSequenceComponentFor call for tcs
      verify(agentUtil, times(1)).spawnSequenceComponentFor(TCS)
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

      when(locationServiceUtil.listAkkaLocationsBy(TCS, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("TCS.primary"), mockAkkaLocation("TCS.secondary")))))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, SequenceComponent))
        .thenReturn(Future.successful(Right(List(mockAkkaLocation("ESW.primary")))))
      when(agentUtil.spawnSequenceComponentFor(TCS))
        .thenReturn(Future.successful(Left(SpawnSequenceComponentFailed("Error in spawning sequence component"))))

      sequenceComponentUtil.getAvailableSequenceComponent(TCS).leftValue shouldBe SpawnSequenceComponentFailed(
        "Error in spawning sequence component"
      )

      // verify call for looking tcs sequence components
      verify(locationServiceUtil).listAkkaLocationsBy(TCS, SequenceComponent)
      // verify call for looking esw sequence components as tcs sequence components are not idle/available
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, SequenceComponent)
      verify(agentUtil, times(1)).spawnSequenceComponentFor(TCS)
    }
  }
}
