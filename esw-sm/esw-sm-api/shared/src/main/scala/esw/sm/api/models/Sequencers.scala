package esw.sm.api.models

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode

/**
 * This class represents an sequencer identifier for an obsMode present in smObsModeConfig.conf
 */
case class SequencerId(subsystem: Subsystem, variation: Option[String] = None) {

  def prefix(obsMode: ObsMode): Prefix =
    variation match {
      case Some(variation) => Prefix(subsystem, s"${obsMode.name}.$variation")
      case None            => Prefix(subsystem, obsMode.name)
    }

  override def toString: String = variation match {
    case Some(variation) => s"$subsystem.$variation"
    case None            => subsystem.name
  }
}

object SequencerId {

  /**
   * Creates SequencerId from given sequencerIdString in smObsModeConfig.conf
   * @param sequencerIdString
   * Possible sequencerIdStrings
   * IRIS.IRIS_IFS.ONE  - Subsystem . variation(IRIS_IFS.ONE)
   *
   * IRIS.IRIS_IMAGER - Subsystem . variation(IRIS_IMAGER)
   *
   * IRIS //Subsystem
   * @return SequencerId
   */
  def fromString(sequencerIdString: String): SequencerId = {
    sequencerIdString.split('.').toList match {
      case Nil =>
        throw new RuntimeException("") //This case will never trigger because split always returns a non-empty array
      case subsystem :: Nil       => SequencerId(Subsystem.withNameInsensitive(subsystem), None)
      case subsystem :: variation => SequencerId(Subsystem.withNameInsensitive(subsystem), Some(variation.mkString(".")))
    }
  }
}

case class Sequencers(sequencerIds: List[SequencerId])
object Sequencers {
  def apply(sequencerIds: SequencerId*): Sequencers = new Sequencers(sequencerIds.toList)
}
