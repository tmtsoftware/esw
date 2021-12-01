package esw.ocs.api.models

import csw.prefix.models.{Prefix, Subsystem}

case class Variation(name: String)

object Variation {
  def prefix(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation]): Prefix = {
    variation match {
      case Some(variation) => Prefix(subsystem, obsMode.name + "." + variation.name)
      case None            => Prefix(subsystem, obsMode.name)
    }
  }
}
