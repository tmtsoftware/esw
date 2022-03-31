/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.models

import csw.prefix.models.{Prefix, Subsystem}

/**
 * This class represents an sequencer identifier for an obsMode present in smObsModeConfig.conf
 */
case class VariationInfo(subsystem: Subsystem, variation: Option[Variation] = None) {

  def prefix(obsMode: ObsMode): Prefix = Variation.prefix(subsystem, obsMode, variation)

  override def toString: String = variation match {
    case Some(variation) => s"$subsystem.${variation.name}"
    case None            => subsystem.name
  }
}

object VariationInfo {

  /**
   * Creates VariationInfo from given variationInfoString in smObsModeConfig.conf
   * @param variationInfoString
   * Possible variationInfoStrings
   * IRIS.IRIS_IFS.ONE  - Subsystem . variation(IRIS_IFS.ONE)
   *
   * IRIS.IRIS_IMAGER - Subsystem . variation(IRIS_IMAGER)
   *
   * IRIS //Subsystem
   * @return VariationInfo
   */
  def from(variationInfoString: String): VariationInfo = {
    variationInfoString.split('.').toList match {
      case Nil => throw new RuntimeException("") // This case will never trigger because split always returns a non-empty array
      case subsystem :: Nil => VariationInfo(Subsystem.withNameInsensitive(subsystem), None)
      case subsystem :: variation =>
        VariationInfo(Subsystem.withNameInsensitive(subsystem), Some(Variation(variation.mkString("."))))
    }
  }
}
