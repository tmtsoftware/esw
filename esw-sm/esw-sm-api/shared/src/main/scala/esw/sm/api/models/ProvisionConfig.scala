package esw.sm.api.models

import csw.prefix.models.Subsystem

case class ProvisionConfig(config: Map[Subsystem, Int]) {
  require(config.forall(_._2 > 0), "Invalid Provision config: Count of sequence components cannot be Zero or less than Zero")
}
