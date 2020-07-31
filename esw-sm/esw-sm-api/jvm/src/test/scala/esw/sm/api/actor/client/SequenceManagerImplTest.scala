package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.models.{ProvisionConfig, SequenceManagerState}
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite

import scala.util.Random

class SequenceManagerImplTest extends BaseTestSuite {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")
  private val configureResponse                                   = mock[ConfigureResponse]
  private val getRunningObsModesResponse                          = mock[GetRunningObsModesResponse]
  private val startSequencerResponse                              = mock[StartSequencerResponse]
  private val shutdownSequencersResponse                          = mock[ShutdownSequencersResponse]
  private val restartSequencerResponse                            = mock[RestartSequencerResponse]
  private val spawnSequenceComponentResponse                      = mock[SpawnSequenceComponentResponse]
  private val shutdownSequenceComponentResponse                   = mock[ShutdownSequenceComponentResponse]
  private val provisionResponse                                   = mock[ProvisionResponse]
  private val getAgentStatusResponse                              = mock[AgentStatusResponse]
  private val smState                                             = mock[SequenceManagerState]
  private val obsMode                                             = ObsMode(randomString5)
  private val seqCompPrefix                                       = Prefix(randomSubsystem, randomString5)
  private val subsystem                                           = randomSubsystem
  private val agent                                               = Prefix(randomSubsystem, randomString5)
  private val seqCompName                                         = randomString5
  private val provisionConfig                                     = ProvisionConfig(Map(seqCompPrefix -> 1))

  private val mockedBehavior: Behaviors.Receive[SequenceManagerMsg] = Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
    msg match {
      case SequenceManagerMsg.Configure(`obsMode`, replyTo)                     => replyTo ! configureResponse
      case SequenceManagerMsg.GetRunningObsModes(replyTo)                       => replyTo ! getRunningObsModesResponse
      case SequenceManagerMsg.GetSequenceManagerState(replyTo)                  => replyTo ! smState
      case SequenceManagerMsg.StartSequencer(`subsystem`, `obsMode`, replyTo)   => replyTo ! startSequencerResponse
      case SequenceManagerMsg.RestartSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! restartSequencerResponse

      case SequenceManagerMsg.ShutdownSequencer(`subsystem`, `obsMode`, replyTo) => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.ShutdownSubsystemSequencers(`subsystem`, replyTo)  => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.ShutdownObsModeSequencers(`obsMode`, replyTo)      => replyTo ! shutdownSequencersResponse
      case SequenceManagerMsg.ShutdownAllSequencers(replyTo)                     => replyTo ! shutdownSequencersResponse

      case SequenceManagerMsg.SpawnSequenceComponent(`agent`, `seqCompName`, replyTo) => replyTo ! spawnSequenceComponentResponse

      case SequenceManagerMsg.ShutdownSequenceComponent(`seqCompPrefix`, replyTo) => replyTo ! shutdownSequenceComponentResponse
      case SequenceManagerMsg.ShutdownAllSequenceComponents(replyTo)              => replyTo ! shutdownSequenceComponentResponse

      case SequenceManagerMsg.Provision(`provisionConfig`, replyTo) => replyTo ! provisionResponse
      case SequenceManagerMsg.GetAllAgentStatus(replyTo)            => replyTo ! getAgentStatusResponse
      case SequenceManagerMsg.ProcessingComplete(_)                 =>
      case msg                                                      => println(s"$msg not handled")
    }
    Behaviors.same
  }

  private val smRef           = system.systemActorOf(mockedBehavior, "sm")
  private val location        = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), smRef.toURI)
  private val sequenceManager = new SequenceManagerImpl(location)

  private def randomString5 = Random.nextString(5)
  private def randomSubsystem = {
    val allSubsystems = Subsystem.values
    allSubsystems(Random.nextInt(allSubsystems.size))
  }

  "SequenceManagerImpl" must {
    "configure" in {
      sequenceManager.configure(obsMode).futureValue shouldBe configureResponse
    }

    "startSequencer" in {
      sequenceManager.startSequencer(subsystem, obsMode).futureValue shouldBe startSequencerResponse
    }

    "restartSequencer" in {
      sequenceManager.restartSequencer(subsystem, obsMode).futureValue shouldBe restartSequencerResponse
    }

    "shutdownSequencer | ESW-326" in {
      sequenceManager.shutdownSequencer(subsystem, obsMode).futureValue shouldBe shutdownSequencersResponse
    }

    "shutdownSubsystemSequencers | ESW-345" in {
      sequenceManager.shutdownSubsystemSequencers(subsystem).futureValue shouldBe shutdownSequencersResponse
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
      sequenceManager.spawnSequenceComponent(agent, seqCompName).futureValue shouldBe spawnSequenceComponentResponse
    }

    "getAgentStatus | ESW-349" in {
      sequenceManager.getAgentStatus.futureValue shouldBe getAgentStatusResponse
    }

    "provision | ESW-346" in {
      sequenceManager.provision(provisionConfig).futureValue shouldBe provisionResponse
    }
  }
}
