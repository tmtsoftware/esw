package esw.sm.api.actor.client

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.testcommons.BaseTestSuite

class SequenceManagerImplTest extends BaseTestSuite {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")
  private val testKit: ActorTestKit                               = ActorTestKit()
  val probe: TestProbe[SequenceManagerMsg]                        = testKit.createTestProbe[SequenceManagerMsg]()
  private val sequenceManager                                     = new SequenceManagerImpl(probe.ref)
  private val obsMode                                             = ObsMode("IRIS_DarkNight")
  private val seqCompPrefix                                       = Prefix(ESW, "primary")

  "SequenceManagerImpl" must {
    "configure" in {
      sequenceManager.configure(obsMode)
      val configure = probe.expectMessageType[Configure]
      configure.obsMode shouldBe obsMode
    }

    "startSequencer" in {
      sequenceManager.startSequencer(ESW, obsMode)
      val startSequencer = probe.expectMessageType[StartSequencer]
      startSequencer.obsMode shouldBe obsMode
      startSequencer.subsystem shouldBe ESW
    }

    "restartSequencer" in {
      sequenceManager.restartSequencer(ESW, obsMode)
      val restartSequencer = probe.expectMessageType[RestartSequencer]
      restartSequencer.obsMode shouldBe obsMode
      restartSequencer.subsystem shouldBe ESW
    }

    "shutdownSequencer | ESW-326" in {
      sequenceManager.shutdownSequencer(ESW, obsMode)
      val shutdownSequencer = probe.expectMessageType[ShutdownSequencer]
      shutdownSequencer.obsMode shouldBe obsMode
      shutdownSequencer.subsystem shouldBe ESW
    }

    "shutdownSubsystemSequencers | ESW-345" in {
      sequenceManager.shutdownSubsystemSequencers(ESW)
      val shutdownSubsystemSequencers = probe.expectMessageType[ShutdownSubsystemSequencers]
      shutdownSubsystemSequencers.subsystem shouldBe ESW
    }

    "shutdownObsModeSequencers | ESW-166" in {
      sequenceManager.shutdownObsModeSequencers(obsMode)
      val shutdownSubsystemSequencers = probe.expectMessageType[ShutdownObsModeSequencers]
      shutdownSubsystemSequencers.obsMode shouldBe obsMode
    }

    "shutdownAllSequencers | ESW-324" in {
      sequenceManager.shutdownAllSequencers()
      probe.expectMessageType[ShutdownAllSequencers]
    }

    "getRunningObsModes" in {
      sequenceManager.getRunningObsModes
      probe.expectMessageType[GetRunningObsModes]
    }

    "shutdownSequenceComponent | ESW-338" in {
      sequenceManager.shutdownSequenceComponent(seqCompPrefix)
      val shutdownSequenceComponent = probe.expectMessageType[ShutdownSequenceComponent]
      shutdownSequenceComponent.prefix shouldBe seqCompPrefix
    }

    "shutdownAllSequenceComponents | ESW-346" in {
      sequenceManager.shutdownAllSequenceComponents()
      probe.expectMessageType[ShutdownAllSequenceComponents]
    }

    "spawnSequenceComponent | ESW-337" in {
      val agent = Prefix("tcs.primary")
      sequenceManager.spawnSequenceComponent(agent, "seq_comp")
      val spawnSequenceComponent = probe.expectMessageType[SpawnSequenceComponent]
      spawnSequenceComponent.machine shouldBe agent
      spawnSequenceComponent.sequenceComponentName shouldBe "seq_comp"
    }

    "getAgentStatus | ESW-349" in {
      sequenceManager.getAgentStatus
      probe.expectMessageType[GetAllAgentStatus]
    }

    "provision | ESW-346" in {
      sequenceManager.provision()
      probe.expectMessageType[Provision]
    }
  }
}
