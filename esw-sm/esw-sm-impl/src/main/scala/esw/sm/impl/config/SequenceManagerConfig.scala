/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.impl.config

import esw.ocs.api.models.ObsMode
import esw.sm.api.models.{Resources, VariationInfos}

//sealed trait ObsModeSequencers
//case class SequencerWithOnlySubsystem(subsystem: Subsystem)                            extends ObsModeSequencers
//case class SequencerWithSubsystemAndVariation(subsystem: Subsystem, variation: String) extends ObsModeSequencers

/**
 * This model class contains the resources and sequencers required for a particular Observing mode
 *
 * @param resources - Resources - set of resources required for the obsMode
 * @param sequencers - Sequencers - set of sequencers required for the obsMode
 */
case class ObsModeConfig(resources: Resources, sequencers: VariationInfos)

/**
 * This model class contains the mapping of ObsMode and their respective obsModeConfigs
 *
 * @param obsModes - Map of ObsMode and ObsModeConfig
 */
case class SequenceManagerConfig(obsModes: Map[ObsMode, ObsModeConfig]) {
  def resources(obsMode: ObsMode): Option[Resources]         = obsModeConfig(obsMode).map(_.resources)
  def sequencers(obsMode: ObsMode): Option[VariationInfos]   = obsModeConfig(obsMode).map(_.sequencers)
  def obsModeConfig(obsMode: ObsMode): Option[ObsModeConfig] = obsModes.get(obsMode)
}
