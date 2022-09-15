/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.impl.core

import csw.command.client.SequencerCommandServiceImpl
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit

class SequencerCommandServiceTest extends EswTestKit {

  "should submit and process sequence | ESW-190, ESW-148" in {
    val sequencerLocation = spawnSequencer(ESW, ObsMode("darknight"))

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val sequence = Sequence(command1)

    val sequencerCommandService = new SequencerCommandServiceImpl(sequencerLocation)
    sequencerCommandService.submitAndWait(sequence).futureValue shouldBe a[Completed]
  }
}
