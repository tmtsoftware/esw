package esw.ocs.api.models

import csw.prefix.models.{Prefix, Subsystem}

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
      case Nil                    => throw new RuntimeException("") //This case will never trigger because split always returns a non-empty array
      case subsystem :: Nil       => SequencerId(Subsystem.withNameInsensitive(subsystem), None)
      case subsystem :: variation => SequencerId(Subsystem.withNameInsensitive(subsystem), Some(variation.mkString(".")))
    }
  }

  /**
   * retrieves ObsMode from given SequencerId
   * @param sequencerPrefix
   * Examples
   * IRIS.IRIS_IFS.ONE  -> ObsMode(IRIS_IFS)
   *
   * IRIS.IRIS_IMAGER -> ObsMode(IRIS_IMAGER)
   *
   * @return SequencerId
   */
  def obsMode(sequencerPrefix: Prefix): ObsMode = {
    sequencerPrefix.componentName.split('.').toList match {
      case Nil          => throw new RuntimeException("") //This case will never trigger because split always returns a non-empty array
      case obsMode :: _ => ObsMode(obsMode)
    }
  }
}
