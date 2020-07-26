package esw.sm.api.actor.client

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite

import scala.util.Random

class SequenceManagerImplTest2 extends BaseTestSuite {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")

  private val askProxyTestKit = new AskProxyTestKit[SequenceManagerMsg, SequenceManagerImpl] {
    override def make(actorRef: ActorRef[SequenceManagerMsg]): SequenceManagerImpl = {
      val location = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), actorRef.toURI)
      new SequenceManagerImpl(location)
    }
  }

  import askProxyTestKit._

  private def randomString5 = Random.nextString(5)
  private def randomSubsystem = {
    val allSubsystems = Subsystem.values
    allSubsystems(Random.nextInt(allSubsystems.size))
  }

  private val obsMode   = ObsMode(randomString5)
  private val subsystem = randomSubsystem

  "SequenceManagerImpl" must {
    "configure" in {
      val configureResponse = mock[ConfigureResponse]
      withBehavior {
        case SequenceManagerMsg.Configure(`obsMode`, replyTo) => replyTo ! configureResponse
      } check { sm =>
        sm.configure(obsMode).futureValue shouldBe configureResponse
      }
    }

    "startSequencer" in {
      val startSequencerResponse = mock[StartSequencerResponse]
      withBehavior {
        case SequenceManagerMsg.StartSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! startSequencerResponse
      } check { sm =>
        sm.startSequencer(subsystem, obsMode).futureValue shouldBe startSequencerResponse
      }
    }

    "restartSequencer" in {
      val restartSequencerResponse = mock[RestartSequencerResponse]
      withBehavior {
        case SequenceManagerMsg.RestartSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! restartSequencerResponse
      } check { sm =>
        sm.restartSequencer(subsystem, obsMode).futureValue shouldBe restartSequencerResponse
      }
    }

    "shutdownSequencer | ESW-326" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSequencer(subsystem, obsMode).futureValue shouldBe shutdownSequencersResponse
      }
    }

    "shutdownSubsystemSequencers | ESW-345" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownSubsystemSequencers(`subsystem`, replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSubsystemSequencers(subsystem).futureValue shouldBe shutdownSequencersResponse
      }
    }

    "shutdownObsModeSequencers | ESW-166" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownObsModeSequencers(`obsMode`, replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownObsModeSequencers(obsMode).futureValue shouldBe shutdownSequencersResponse
      }
    }

    "shutdownAllSequencers | ESW-324" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownAllSequencers(replyTo) => replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownAllSequencers().futureValue shouldBe shutdownSequencersResponse
      }
    }

    "getRunningObsModes" in {
      val getRunningObsModesResponse = mock[GetRunningObsModesResponse]
      withBehavior {
        case SequenceManagerMsg.GetRunningObsModes(replyTo) => replyTo ! getRunningObsModesResponse
      } check { sm =>
        sm.getRunningObsModes.futureValue shouldBe getRunningObsModesResponse
      }
    }

    "shutdownSequenceComponent | ESW-338" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      val seqCompPrefix                     = Prefix(randomSubsystem, randomString5)
      withBehavior {
        case SequenceManagerMsg.ShutdownSequenceComponent(`seqCompPrefix`, replyTo) => replyTo ! shutdownSequenceComponentResponse
      } check { sm =>
        sm.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe shutdownSequenceComponentResponse
      }
    }

    "shutdownAllSequenceComponents | ESW-346" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      withBehavior {
        case SequenceManagerMsg.ShutdownAllSequenceComponents(replyTo) => replyTo ! shutdownSequenceComponentResponse
      } check { sm =>
        sm.shutdownAllSequenceComponents().futureValue shouldBe shutdownSequenceComponentResponse
      }
    }

    "spawnSequenceComponent | ESW-337" in {
      val spawnSeqCompResponse = mock[SpawnSequenceComponentResponse]
      val agent                = Prefix(randomSubsystem, randomString5)
      val seqCompName          = randomString5
      withBehavior {
        case SequenceManagerMsg.SpawnSequenceComponent(`agent`, `seqCompName`, replyTo) => replyTo ! spawnSeqCompResponse
      } check { sm =>
        sm.spawnSequenceComponent(agent, seqCompName).futureValue shouldBe spawnSeqCompResponse
      }
    }

    "getAgentStatus | ESW-349" in {
      val agentStatusResponse = mock[AgentStatusResponse]
      withBehavior {
        case SequenceManagerMsg.GetAllAgentStatus(replyTo) => replyTo ! agentStatusResponse
      } check { sm =>
        sm.getAgentStatus.futureValue shouldBe agentStatusResponse
      }
    }

    "provision | ESW-346" in {
      val provisionResponse = mock[ProvisionResponse]
      withBehavior {
        case SequenceManagerMsg.Provision(replyTo) => replyTo ! provisionResponse
      } check { sm =>
        sm.provision().futureValue shouldBe provisionResponse
      }
    }
  }
}
