/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.models

import csw.prefix.models.Prefix

/**
 * Model which represents the observation mode of the sequencer
 */
case class ObsMode(name: String)

object ObsMode {

  /**
   * retrieves ObsMode from given Sequencer Prefix
   * @param sequencerPrefix
   * Examples
   * IRIS.IRIS_IFS.ONE  -> ObsMode(IRIS_IFS)
   *
   * IRIS.IRIS_IMAGER -> ObsMode(IRIS_IMAGER)
   *
   * @return SequencerId
   */
  def from(sequencerPrefix: Prefix): ObsMode = {
    sequencerPrefix.componentName.split('.').toList match {
      case Nil => throw new RuntimeException("") // This case will never trigger because split always returns a non-empty array
      case obsMode :: _ => ObsMode(obsMode)
    }
  }
}
