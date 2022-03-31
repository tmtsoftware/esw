/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.impl.core

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.actor.messages.SequencerMessages.{LoadSequence, SubmitSequenceInternal}
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.{OkOrUnhandledResponse, SequencerSubmitResponse, SubmitResult, Unhandled}
import esw.ocs.testkit.EswTestKit

class SequencerBehaviorIntegrationTest extends EswTestKit {
  private val ocsSubsystem = ESW
  private val ocsObsMode   = ObsMode("darknight")

  "Sequencer" must {
    "not receive sequence when already processing a sequence | ESW-145" in {
      val command                   = Setup(Prefix("TCS.test"), CommandName("test-sequencer-hierarchy"), None)
      val submitResponseProbe       = TestProbe[SequencerSubmitResponse]()
      val loadSequenceResponseProbe = TestProbe[OkOrUnhandledResponse]()
      val sequence                  = Sequence(command)
      val ocsSequencer              = spawnSequencerRef(ocsSubsystem, ocsObsMode)

      ocsSequencer ! SubmitSequenceInternal(sequence, submitResponseProbe.ref)
      Thread.sleep(1000)
      ocsSequencer ! LoadSequence(sequence, loadSequenceResponseProbe.ref)

      // response received by irisSequencer
      submitResponseProbe.expectMessageType[SubmitResult]
      loadSequenceResponseProbe.expectMessage(Unhandled("Running", "LoadSequence"))
    }
  }
}
