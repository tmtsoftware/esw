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
  private val sequenceComponentId: ComponentId                          = ComponentId(Prefix("tcs.seq_comp"), ComponentType.SequenceComponent)
  private val configureResponse                                         = ConfigureResponse.Success(sequencerComponentId)
  private val getRunningObsModesResponse                                = GetRunningObsModesResponse.Success(Set(ObsMode("IRIS_Darknight"), ObsMode("WFOS_cal")))
  private val startSequencerResponse                                    = StartSequencerResponse.Started(sequencerComponentId)
  private val shutdownSequencersResponse                                = ShutdownSequencersResponse.Success
  private val restartSequencerResponse                                  = RestartSequencerResponse.Success(sequencerComponentId)
  private val spawnSequenceComponentResponse                            = SpawnSequenceComponentResponse.Success(sequenceComponentId)
  private val shutdownSequenceComponentResponse                         = ShutdownSequenceComponentResponse.Success

  private val mockedBehavior: Behaviors.Receive[SequenceManagerMsg] = Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
    msg match {
      case SequenceManagerMsg.Configure(_, replyTo)                 => replyTo ! configureResponse
      case SequenceManagerMsg.GetRunningObsModes(replyTo)           => replyTo ! getRunningObsModesResponse
      case SequenceManagerMsg.GetSequenceManagerState(replyTo)      => replyTo ! Idle
      case SequenceManagerMsg.StartSequencer(_, _, replyTo)         => replyTo ! startSequencerResponse
      case SequenceManagerMsg.ShutdownSequencers(_, _, _, replyTo)  => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.RestartSequencer(_, _, replyTo)       => replyTo ! restartSequencerResponse
      case SequenceManagerMsg.SpawnSequenceComponent(_, _, replyTo) => replyTo ! spawnSequenceComponentResponse
      case SequenceManagerMsg.ShutdownSequenceComponent(_, replyTo) => replyTo ! shutdownSequenceComponentResponse
      case SequenceManagerMsg.StartSequencerResponseInternal(_)     =>
      case SequenceManagerMsg.ShutdownSequencersResponseInternal(_) =>
      case SequenceManagerMsg.RestartSequencerResponseInternal(_)   =>
      case SequenceManagerMsg.ConfigurationResponseInternal(_)      =>
      case SequenceManagerMsg.SpawnSequenceComponentInternal(_)     =>
      case SequenceManagerMsg.ShutdownSequenceComponentInternal(_)  =>
    }
    Behaviors.same
  }

  private val smRef           = system.systemActorOf(mockedBehavior, "sm")
  private val location        = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), smRef.toURI)
  private val sequenceManager = new SequenceManagerImpl(location)
  private val obsMode         = ObsMode("IRIS_darknight")
  private val seqCompPrefix   = Prefix(ESW, "primary")

  "SequenceManagerImpl" must {
    "configure" in {
      sequenceManager.configure(obsMode).futureValue shouldBe configureResponse
    }

    "getRunningObsModes" in {
      sequenceManager.getRunningObsModes.futureValue shouldBe getRunningObsModesResponse
    }

    "startSequencer" in {
      sequenceManager.startSequencer(ESW, obsMode).futureValue shouldBe startSequencerResponse
    }

    "shutdownSequencers" in {
      sequenceManager.shutdownSequencers(Some(ESW), Some(obsMode)).futureValue shouldBe shutdownSequencersResponse
    }

    "shutdownSequencers (all)" in {
      sequenceManager.shutdownSequencers(None, None).futureValue shouldBe shutdownSequencersResponse
    }

    "shutdownSequenceComponent | ESW-338" in {
      sequenceManager.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe shutdownSequenceComponentResponse
    }

    "spawnSequenceComponent | ESW-337" in {
      val machine = ComponentId(Prefix("tcs.primary"), ComponentType.Machine)
      sequenceManager.spawnSequenceComponent(machine, "seq_comp").futureValue shouldBe spawnSequenceComponentResponse
    "restartSequencer" in {
      sequenceManager.restartSequencer(ESW, obsMode).futureValue shouldBe restartSequencerResponse
    }
  }
}
