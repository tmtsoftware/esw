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
import esw.sm.api.protocol.AgentStatusResponses.AgentStatus
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite

class SequenceManagerImplTest extends BaseTestSuite {
  private final implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")
  private val sequencerComponentId: ComponentId                         = ComponentId(Prefix("esw.primary"), ComponentType.Sequencer)
  private val sequenceComponentId: ComponentId                          = ComponentId(Prefix("tcs.seq_comp"), ComponentType.SequenceComponent)
  private val configureResponse                                         = ConfigureResponse.Success(sequencerComponentId)
  private val getRunningObsModesResponse                                = GetRunningObsModesResponse.Success(Set(ObsMode("IRIS_DarkNight"), ObsMode("WFOS_cal")))
  private val startSequencerResponse                                    = StartSequencerResponse.Started(sequencerComponentId)
  private val shutdownSequencersResponse                                = ShutdownSequencersResponse.Success
  private val restartSequencerResponse                                  = RestartSequencerResponse.Success(sequencerComponentId)
  private val spawnSequenceComponentResponse                            = SpawnSequenceComponentResponse.Success(sequenceComponentId)
  private val shutdownSequenceComponentResponse                         = ShutdownSequenceComponentResponse.Success
  private val provisionResponse                                         = ProvisionResponse.Success
  private val getAgentStatusResponse                                    = AgentStatusResponse.Success(List.empty[AgentStatus])

  private val mockedBehavior: Behaviors.Receive[SequenceManagerMsg] = Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
    msg match {
      case SequenceManagerMsg.Configure(_, replyTo)            => replyTo ! configureResponse
      case SequenceManagerMsg.GetRunningObsModes(replyTo)      => replyTo ! getRunningObsModesResponse
      case SequenceManagerMsg.GetSequenceManagerState(replyTo) => replyTo ! Idle
      case SequenceManagerMsg.StartSequencer(_, _, replyTo)    => replyTo ! startSequencerResponse
      case SequenceManagerMsg.RestartSequencer(_, _, replyTo)  => replyTo ! restartSequencerResponse

      case SequenceManagerMsg.ShutdownSequencer(_, _, replyTo)        => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.ShutdownSubsystemSequencers(_, replyTo) => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.ShutdownObsModeSequencers(_, replyTo)   => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.ShutdownAllSequencers(replyTo)          => replyTo ! shutdownSequencersResponse

      case SequenceManagerMsg.SpawnSequenceComponent(_, _, replyTo) => replyTo ! spawnSequenceComponentResponse

      case SequenceManagerMsg.ShutdownSequenceComponent(_, replyTo)  => replyTo ! shutdownSequenceComponentResponse
      case SequenceManagerMsg.ShutdownAllSequenceComponents(replyTo) => replyTo ! shutdownSequenceComponentResponse

      case SequenceManagerMsg.Provision(replyTo)      => replyTo ! provisionResponse
      case SequenceManagerMsg.GetAgentStatus(replyTo) => replyTo ! getAgentStatusResponse
      case SequenceManagerMsg.ProcessingComplete(_)   =>
    }
    Behaviors.same
  }

  private val smRef           = system.systemActorOf(mockedBehavior, "sm")
  private val location        = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), smRef.toURI)
  private val sequenceManager = new SequenceManagerImpl(location)
  private val obsMode         = ObsMode("IRIS_DarkNight")
  private val seqCompPrefix   = Prefix(ESW, "primary")

  "SequenceManagerImpl" must {
    "configure" in {
      sequenceManager.configure(obsMode).futureValue shouldBe configureResponse
    }

    "startSequencer" in {
      sequenceManager.startSequencer(ESW, obsMode).futureValue shouldBe startSequencerResponse
    }

    "restartSequencer" in {
      sequenceManager.restartSequencer(ESW, obsMode).futureValue shouldBe restartSequencerResponse
    }

    "shutdownSequencer | ESW-326" in {
      sequenceManager.shutdownSequencer(ESW, obsMode).futureValue shouldBe shutdownSequencersResponse
    }

    "shutdownSubsystemSequencers | ESW-345" in {
      sequenceManager.shutdownSubsystemSequencers(ESW).futureValue shouldBe shutdownSequencersResponse
    }

    "shutdownObsModeSequencers | ESW-166" in {
      sequenceManager.shutdownObsModeSequencers(obsMode).futureValue shouldBe shutdownSequencersResponse
    }

    "shutdownAllSequencers | ESW-324" in {
      sequenceManager.shutdownAllSequencers().futureValue shouldBe shutdownSequencersResponse
    }

    "getRunningObsModes" in {
      sequenceManager.getRunningObsModes.futureValue shouldBe getRunningObsModesResponse
    }

    "shutdownSequenceComponent | ESW-338" in {
      sequenceManager.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe shutdownSequenceComponentResponse
    }

    "shutdownAllSequenceComponents | ESW-346" in {
      sequenceManager.shutdownAllSequenceComponents().futureValue shouldBe shutdownSequenceComponentResponse
    }

    "spawnSequenceComponent | ESW-337" in {
      val agent = Prefix("tcs.primary")
      sequenceManager.spawnSequenceComponent(agent, "seq_comp").futureValue shouldBe spawnSequenceComponentResponse
    }

    "getAgentStatus | ESW-349" in {
      sequenceManager.getAgentStatus.futureValue shouldBe getAgentStatusResponse
    }

    "provision | ESW-346" in {
      sequenceManager.provision().futureValue shouldBe provisionResponse
    }
  }
}
