package esw.sm.api.models

import esw.ocs.api.models.VariationId

case class VariationIds(variationIds: List[VariationId])
object VariationIds {
  def apply(variationIds: VariationId*): VariationIds = new VariationIds(variationIds.toList)

  def empty: VariationIds = VariationIds()
}
