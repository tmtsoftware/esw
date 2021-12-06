package esw.sm.api.models

import csw.prefix.models.Prefix

case class Sequencers(sequencers: List[Prefix])
object Sequencers {
  def apply(sequencers: Prefix*): Sequencers = new Sequencers(sequencers.toList)
}
