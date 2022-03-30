/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.app

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.GetStatusResponse
import esw.ocs.testkit.utils.LocationUtils

trait SequenceManagerSetup extends LocationUtils {
  val WFOS_CAL: ObsMode             = ObsMode("WFOS_Cal")
  val IRIS_CAL: ObsMode             = ObsMode("IRIS_Cal")
  val IRIS_DARKNIGHT: ObsMode       = ObsMode("IRIS_Darknight")
  val sequenceManagerPrefix: Prefix = Prefix(ESW, "sequence_manager")
  val ocsVersionOpt: Option[String] = Some("0.1.0-SNAPSHOT")

  def sequencerConnection(prefix: Prefix): AkkaConnection = AkkaConnection(ComponentId(prefix, Sequencer))

  def assertThatSeqCompIsAvailable(prefix: Prefix): Unit = assertSeqCompAvailability(isSeqCompAvailable = true, prefix)

  def assertThatSeqCompIsLoadedWithScript(prefix: Prefix): Unit =
    assertSeqCompAvailability(isSeqCompAvailable = false, prefix)

  def assertSeqCompAvailability(isSeqCompAvailable: Boolean, prefix: Prefix): Unit = {
    val seqCompStatus = new SequenceComponentImpl(resolveSequenceComponentLocation(prefix)).status.futureValue
    seqCompStatus shouldBe a[GetStatusResponse]
    val getStatusResponse = seqCompStatus.asInstanceOf[GetStatusResponse]
    if (isSeqCompAvailable) getStatusResponse.response shouldBe None // assert sequence component is available
    else getStatusResponse.response.isDefined shouldBe true          // assert sequence components is busy
  }
}
