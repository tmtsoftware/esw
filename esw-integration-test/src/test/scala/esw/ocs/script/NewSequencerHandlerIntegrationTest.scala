package esw.ocs.script

import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventKey, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.LGSF
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.testkit.EswTestKit

class NewSequencerHandlerIntegrationTest extends EswTestKit(EventServer) {

  private val obsMode = "darknight"
  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnSequencer(LGSF, obsMode)
  }

  "Sequencer" must {
    "run the new Sequencer handler before starting the new Sequence | ESW-303" in {
      val sequencer = sequencerClient(LGSF, obsMode)
      val command   = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence  = Sequence(Seq(command))

      //create testprobe for the event publishing in command-1 handler in the script
      val newSequenceEventKeys           = Set(EventKey("LGSF.darknight.NewSequence"))
      val newSequenceInitializationProbe = createTestProbe(newSequenceEventKeys)

      //create testprobe for the event publishing in onNewSequence handler in the script
      val newSequenceHandlerEventKeys           = Set(EventKey("LGSF.darknight.NewSequenceHandler"))
      val newSequenceHandlerInitializationProbe = createTestProbe(newSequenceHandlerEventKeys)

      val submitResponseF = sequencer.submit(sequence)

      //assert sequence has not initialize
      newSequenceInitializationProbe.expectNoMessage()

      val sequenceHandlerEventParam          = StringKey.make("onNewSequence").set("Started")
      val sequenceHandlerInitializationEvent = newSequenceHandlerInitializationProbe.expectMessageType[SystemEvent]
      //assert onNewSequence handler has started
      sequenceHandlerInitializationEvent.paramSet.head shouldBe sequenceHandlerEventParam

      Thread.sleep(500)
      //onNewSequence handler completed

      val newSequenceEventParam       = StringKey.make("sequence-command-1").set("Started")
      val sequenceInitializationEvent = newSequenceInitializationProbe.expectMessageType[SystemEvent]
      //assert sequence has started
      sequenceInitializationEvent.paramSet.head shouldBe newSequenceEventParam

      submitResponseF.futureValue shouldBe a[Started]
    }
  }
}
