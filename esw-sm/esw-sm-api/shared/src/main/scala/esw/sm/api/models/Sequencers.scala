package esw.sm.api.models

import csw.prefix.models.Subsystem

//todo: subsystems in sequencers cannot be duplicate (replace List by Set)
case class Sequencers(subsystems: List[Subsystem])
object Sequencers {
  def apply(subsystems: Subsystem*): Sequencers = new Sequencers(subsystems.toList)
}
