package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerState.Idle
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite

class SequenceManagerImplTest extends BaseTestSuite {
  private final implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")
  private val sequencerComponentId: ComponentId                         = ComponentId(Prefix("esw.primary"), ComponentType.Sequencer)
  private val configureResponse                                         = ConfigureResponse.Success(sequencerComponentId)
  private val cleanupResponse                                           = CleanupResponse.Success
  private val getRunningObsModesResponse                                = GetRunningObsModesResponse.Success(Set(ObsMode("IRIS_Darknight"), ObsMode("WFOS_cal")))
  private val startSequencerResponse                                    = StartSequencerResponse.Started(sequencerComponentId)
  private val shutdownSequencerResponse                                 = ShutdownSequencerResponse.Success
  private val shutdownAllSequencersResponse                             = ShutdownAllSequencersResponse.Success
  private val restartSequencerResponse                                  = RestartSequencerResponse.Success(sequencerComponentId)
  private val shutdownSequenceComponentResponse                         = ShutdownSequenceComponentResponse.Success

  private val mockedBehavior: Behaviors.Receive[SequenceManagerMsg] = Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
    msg match {
      case SequenceManagerMsg.Configure(_, replyTo)                    => replyTo ! configureResponse
      case SequenceManagerMsg.Cleanup(_, replyTo)                      => replyTo ! cleanupResponse
      case SequenceManagerMsg.GetRunningObsModes(replyTo)              => replyTo ! getRunningObsModesResponse
      case SequenceManagerMsg.GetSequenceManagerState(replyTo)         => replyTo ! Idle
      case SequenceManagerMsg.StartSequencer(_, _, replyTo)            => replyTo ! startSequencerResponse
      case SequenceManagerMsg.ShutdownSequencer(_, _, _, replyTo)      => replyTo ! shutdownSequencerResponse
      case SequenceManagerMsg.ShutdownAllSequencers(replyTo)           => replyTo ! shutdownAllSequencersResponse
      case SequenceManagerMsg.RestartSequencer(_, _, replyTo)          => replyTo ! restartSequencerResponse
      case SequenceManagerMsg.ShutdownSequenceComponent(_, replyTo)    => replyTo ! shutdownSequenceComponentResponse
      case SequenceManagerMsg.StartSequencerResponseInternal(_)        =>
      case SequenceManagerMsg.ShutdownAllSequencersResponseInternal(_) =>
      case SequenceManagerMsg.ShutdownSequencerResponseInternal(_)     =>
      case SequenceManagerMsg.RestartSequencerResponseInternal(_)      =>
      case SequenceManagerMsg.CleanupResponseInternal(_)               =>
      case SequenceManagerMsg.ConfigurationResponseInternal(_)         =>
      case SequenceManagerMsg.ShutdownSequenceComponentInternal(_)     =>
    }
    Behaviors.same
  }

  private val smRef           = system.systemActorOf(mockedBehavior, "sm")
  private val location        = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), smRef.toURI)
  private val sequenceManager = new SequenceManagerImpl(location)
  private val obsMode         = ObsMode("IRIS_darknight")

  "SequenceManagerImpl" must {
    "configure" in {
      sequenceManager.configure(obsMode).futureValue shouldBe configureResponse
    }

    "cleanup" in {
      sequenceManager.cleanup(obsMode).futureValue shouldBe cleanupResponse
    }

    "getRunningObsModes" in {
      sequenceManager.getRunningObsModes.futureValue shouldBe getRunningObsModesResponse
    }

    "startSequencer" in {
      sequenceManager.startSequencer(ESW, obsMode).futureValue shouldBe startSequencerResponse
    }

    "shutdownSequencer" in {
      sequenceManager.shutdownSequencer(ESW, obsMode).futureValue shouldBe shutdownSequencerResponse
    }

    "restartSequencer" in {
      sequenceManager.restartSequencer(ESW, obsMode).futureValue shouldBe restartSequencerResponse
    }

    "shutdownAllSequencers" in {
      sequenceManager.shutdownAllSequencers().futureValue shouldBe shutdownAllSequencersResponse
    }
  }
}
