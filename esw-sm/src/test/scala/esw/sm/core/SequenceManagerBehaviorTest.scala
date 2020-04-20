package esw.sm.core

import java.net.URI

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.BaseTestSuite
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.messages.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.messages.SequenceManagerMsg.Configure
import esw.sm.messages.{ConfigureResponse, SequenceManagerMsg}
import esw.sm.utils.SequencerUtil

import scala.concurrent.Future

class SequenceManagerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  val DARKNIGHT                                = "darknight"
  val CLEARSKIES                               = "clearskies"
  private val darknightSequencers: Sequencers  = Sequencers(ESW, TCS)
  private val clearskiesSequencers: Sequencers = Sequencers(TCS)
  private val config = Map(
    DARKNIGHT  -> ObsModeConfig(Resources("r1", "r2"), darknightSequencers),
    CLEARSKIES -> ObsModeConfig(Resources("r2", "r3"), clearskiesSequencers)
  )
  private val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil             = mock[SequencerUtil]
  private val sequenceManagerBehavior                  = new SequenceManagerBehavior(config, locationServiceUtil, sequencerUtil)

  private val smRef: ActorRef[SequenceManagerMsg] = system.systemActorOf(sequenceManagerBehavior.behavior(), "test_actor")

  override protected def beforeEach(): Unit = {
    reset(locationServiceUtil)
    reset(sequencerUtil)
  }

  "configure" must {
    "start sequence hierarchy and return master sequencer" in {
      val httpLocation = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listBy(ESW, Sequencer)).thenReturn(Future.successful(List.empty))
      when(sequencerUtil.startSequencers(DARKNIGHT, darknightSequencers)).thenReturn(Future.successful(Success(httpLocation)))
      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(None))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure("darknight", probe.ref)

      probe.expectMessage(Success(httpLocation))
      verify(sequencerUtil).resolveMasterSequencerOf(DARKNIGHT)
      verify(locationServiceUtil).listBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(DARKNIGHT, darknightSequencers)
    }

    "return resource conflict error when required resources are already in use" in {
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, CLEARSKIES), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listBy(ESW, Sequencer)).thenReturn(Future.successful(List(akkaLocation)))
      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(None))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure("darknight", probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode)
      verify(sequencerUtil).resolveMasterSequencerOf(DARKNIGHT)
      verify(locationServiceUtil).listBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(DARKNIGHT, darknightSequencers)
    }
  }

}
