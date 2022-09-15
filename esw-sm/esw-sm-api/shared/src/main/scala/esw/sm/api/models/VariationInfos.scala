/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api.models

import esw.ocs.api.models.VariationInfo

case class VariationInfos(variationInfos: List[VariationInfo])
object VariationInfos {
  def apply(variationInfos: VariationInfo*): VariationInfos = new VariationInfos(variationInfos.toList)

  def empty: VariationInfos = VariationInfos()
}
