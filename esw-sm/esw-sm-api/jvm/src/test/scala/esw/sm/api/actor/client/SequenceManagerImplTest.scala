package esw.sm.api.actor.client

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol._
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

class SequenceManagerImplTest extends ActorTestSuit {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")

  private val askProxyTestKit = new AskProxyTestKit[SequenceManagerMsg, SequenceManagerImpl] {
    override def make(actorRef: ActorRef[SequenceManagerMsg]): SequenceManagerImpl = {
      val location =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), actorRef.toURI, Metadata.empty)
      new SequenceManagerImpl(location)
    }
  }

  import askProxyTestKit._

  private def randomString5 = randomString(5)

  private val obsMode   = ObsMode(randomString5)
  private val subsystem = randomSubsystem

  "SequenceManagerImpl" must {
    "configure" in {
      val configureResponse = mock[ConfigureResponse]
      withBehavior {
        case SequenceManagerMsg.Configure(`obsMode`, replyTo) => replyTo ! configureResponse
      } check { sm =>
        sm.configure(obsMode).futureValue should ===(configureResponse)
      }
    }

    "startSequencer" in {
      val startSequencerResponse = mock[StartSequencerResponse]
      withBehavior {
        case SequenceManagerMsg.StartSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! startSequencerResponse
      } check { sm =>
        sm.startSequencer(subsystem, obsMode).futureValue should ===(startSequencerResponse)
      }
    }

    "restartSequencer" in {
      val restartSequencerResponse = mock[RestartSequencerResponse]
      withBehavior {
        case SequenceManagerMsg.RestartSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! restartSequencerResponse
      } check { sm =>
        sm.restartSequencer(subsystem, obsMode).futureValue should ===(restartSequencerResponse)
      }
    }

    "shutdownSequencer | ESW-326" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSequencer(subsystem, obsMode).futureValue should ===(shutdownSequencersResponse)
      }
    }

    "shutdownSubsystemSequencers | ESW-345" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownSubsystemSequencers(`subsystem`, replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSubsystemSequencers(subsystem).futureValue should ===(shutdownSequencersResponse)
      }
    }

    "shutdownObsModeSequencers | ESW-166" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownObsModeSequencers(`obsMode`, replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownObsModeSequencers(obsMode).futureValue should ===(shutdownSequencersResponse)
      }
    }

    "shutdownAllSequencers | ESW-324" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownAllSequencers(replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownAllSequencers().futureValue should ===(shutdownSequencersResponse)
      }
    }

    "getRunningObsModes" in {
      val getRunningObsModesResponse = mock[GetRunningObsModesResponse]
      withBehavior {
        case SequenceManagerMsg.GetRunningObsModes(replyTo) => replyTo ! getRunningObsModesResponse
      } check { sm =>
        sm.getRunningObsModes.futureValue should ===(getRunningObsModesResponse)
      }
    }

    "shutdownSequenceComponent | ESW-338" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      val seqCompPrefix                     = Prefix(randomSubsystem, randomString5)
      withBehavior {
        case SequenceManagerMsg.ShutdownSequenceComponent(`seqCompPrefix`, replyTo) => replyTo ! shutdownSequenceComponentResponse
      } check { sm =>
        sm.shutdownSequenceComponent(seqCompPrefix).futureValue should ===(shutdownSequenceComponentResponse)
      }
    }

    "shutdownAllSequenceComponents | ESW-346" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownAllSequenceComponents(replyTo) => replyTo ! shutdownSequenceComponentResponse
      } check { sm =>
        sm.shutdownAllSequenceComponents().futureValue should ===(shutdownSequenceComponentResponse)
      }
    }

    "getAgentStatus | ESW-349" in {
      val agentStatusResponse = mock[AgentStatusResponse]
      withBehavior {
        case SequenceManagerMsg.GetAllAgentStatus(replyTo) => replyTo ! agentStatusResponse
      } check { sm =>
        sm.getAgentStatus.futureValue should ===(agentStatusResponse)
      }
    }

    "provision | ESW-346" in {
      val provisionResponse = mock[ProvisionResponse]
      val provisionConfig   = ProvisionConfig((Prefix(randomSubsystem, randomString5), randomInt(5)))
      withBehavior {
        case SequenceManagerMsg.Provision(`provisionConfig`, replyTo) => replyTo ! provisionResponse
      } check { sm =>
        sm.provision(provisionConfig).futureValue should ===(provisionResponse)
      }
    }
  }
}
