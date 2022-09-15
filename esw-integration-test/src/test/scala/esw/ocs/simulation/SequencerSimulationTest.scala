/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.simulation

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventKey, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit

class SequencerSimulationTest extends EswTestKit(EventServer) {

  private val obsMode = ObsMode("moonnight")

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnSequencer(TCS, obsMode)
    spawnSequencerInSimulation(ESW, obsMode)
  }

  "Sequencer in simulation | ESW-149" in {
    val sequencer = sequencerClient(ESW, obsMode)

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
    val sequence = Sequence(command1, command2)

    sequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]
  }

  "submit sequence from a top level sequencer to sequencer in simulation | ESW-149" in {
    // creating client for TCS.moonnight(Top level) sequencer
    val tcsSequencer = sequencerClient(TCS, obsMode)

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
    val sequence = Sequence(command1, command2)

    val eventKey = EventKey("ESW." + obsMode.name + ".submitAndWait")
    // create testprobe subscription for submitResponse(res of submitAndWait command to the simulation sequencer in testscript3) event
    val testProbeForSimulation = createTestProbe(Set(eventKey))

    // submitting sequence to the top level sequencer(TCS.moonnight)
    tcsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

    // actual event published from the testscript3 command-1 handler
    val event = testProbeForSimulation.expectMessageType[SystemEvent]

    val expectedSubmitResponseParam = StringKey.make("response").set("Completed")
    event.paramSet.head shouldBe expectedSubmitResponseParam
  }
}
