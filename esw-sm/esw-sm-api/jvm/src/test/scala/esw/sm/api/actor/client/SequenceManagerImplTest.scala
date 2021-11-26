package esw.sm.api.actor.client

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension.*
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.*
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

  import askProxyTestKit.*

  private val obsMode   = ObsMode(randomString5)
  private val subsystem = randomSubsystem
  private val prefix    = Prefix(subsystem, obsMode.name)

  "SequenceManagerImpl" must {
    "configure | ESW-362" in {
      val configureResponse = mock[ConfigureResponse]
      withBehavior { case SequenceManagerMsg.Configure(`obsMode`, replyTo) =>
        replyTo ! configureResponse
      } check { sm =>
        sm.configure(obsMode).futureValue should ===(configureResponse)
      }
    }

    "startSequencer | ESW-362, ESW-561" in {
      val startSequencerResponse = mock[StartSequencerResponse]
      withBehavior { case SequenceManagerMsg.StartSequencer(`prefix`, replyTo) =>
        replyTo ! startSequencerResponse
      } check { sm =>
        sm.startSequencer(prefix).futureValue should ===(startSequencerResponse)
      }
    }

    "restartSequencer | ESW-362, ESW-561" in {
      val restartSequencerResponse = mock[RestartSequencerResponse]
      withBehavior { case SequenceManagerMsg.RestartSequencer(`prefix`, replyTo) =>
        replyTo ! restartSequencerResponse
      } check { sm =>
        sm.restartSequencer(prefix).futureValue should ===(restartSequencerResponse)
      }
    }

    "shutdownSequencer | ESW-326, ESW-362, ESW-561" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior { case SequenceManagerMsg.ShutdownSequencer(`prefix`, replyTo) =>
        replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSequencer(prefix).futureValue should ===(shutdownSequencersResponse)
      }
    }

    "shutdownSubsystemSequencers | ESW-345, ESW-362" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior { case SequenceManagerMsg.ShutdownSubsystemSequencers(`subsystem`, replyTo) =>
        replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSubsystemSequencers(subsystem).futureValue should ===(shutdownSequencersResponse)
      }
    }

    "shutdownObsModeSequencers | ESW-166, ESW-362" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior { case SequenceManagerMsg.ShutdownObsModeSequencers(`obsMode`, replyTo) =>
        replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownObsModeSequencers(obsMode).futureValue should ===(shutdownSequencersResponse)
      }
    }

    "shutdownAllSequencers | ESW-324, ESW-362" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior { case SequenceManagerMsg.ShutdownAllSequencers(replyTo) =>
        replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownAllSequencers().futureValue should ===(shutdownSequencersResponse)
      }
    }

    "getObsModesDetails | ESW-466" in {
      val obsModesDetailsResponse = mock[ObsModesDetailsResponse]
      withBehavior { case SequenceManagerMsg.GetObsModesDetails(replyTo) =>
        replyTo ! obsModesDetailsResponse
      } check { sm =>
        sm.getObsModesDetails.futureValue should ===(obsModesDetailsResponse)
      }
    }

    "shutdownSequenceComponent | ESW-338, ESW-362" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      val seqCompPrefix                     = Prefix(randomSubsystem, randomString5)
      withBehavior { case SequenceManagerMsg.ShutdownSequenceComponent(`seqCompPrefix`, replyTo) =>
        replyTo ! shutdownSequenceComponentResponse
      } check { sm =>
        sm.shutdownSequenceComponent(seqCompPrefix).futureValue should ===(shutdownSequenceComponentResponse)
      }
    }

    "shutdownAllSequenceComponents | ESW-346, ESW-362" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      withBehavior { case SequenceManagerMsg.ShutdownAllSequenceComponents(replyTo) =>
        replyTo ! shutdownSequenceComponentResponse
      } check { sm =>
        sm.shutdownAllSequenceComponents().futureValue should ===(shutdownSequenceComponentResponse)
      }
    }

    "provision | ESW-346, ESW-362" in {
      val provisionResponse = mock[ProvisionResponse]
      val provisionConfig   = ProvisionConfig((Prefix(randomSubsystem, randomString5), randomInt(5)))
      withBehavior { case SequenceManagerMsg.Provision(`provisionConfig`, replyTo) =>
        replyTo ! provisionResponse
      } check { sm =>
        sm.provision(provisionConfig).futureValue should ===(provisionResponse)
      }
    }

    "getResourceStatus | ESW-467" in {
      val response = mock[ResourcesStatusResponse]
      withBehavior { case SequenceManagerMsg.GetResources(replyTo) =>
        replyTo ! response
      } check { sm =>
        sm.getResources.futureValue should ===(response)
      }
    }
  }
}
