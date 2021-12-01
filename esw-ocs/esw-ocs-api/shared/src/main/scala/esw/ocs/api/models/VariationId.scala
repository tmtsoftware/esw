package esw.ocs.api.models

import csw.prefix.models.{Prefix, Subsystem}

/**
 * This class represents an sequencer identifier for an obsMode present in smObsModeConfig.conf
 */
case class VariationId(subsystem: Subsystem, variation: Option[Variation] = None) {

  def prefix(obsMode: ObsMode): Prefix = Variation.prefix(subsystem, obsMode, variation)

  override def toString: String = variation match {
    case Some(variation) => s"$subsystem.$variation"
    case None            => subsystem.name
  }
}

object VariationId {

  /**
   * Creates VariationId from given variationIdString in smObsModeConfig.conf
   * @param variationIdString
   * Possible variationIdStrings
   * IRIS.IRIS_IFS.ONE  - Subsystem . variation(IRIS_IFS.ONE)
   *
   * IRIS.IRIS_IMAGER - Subsystem . variation(IRIS_IMAGER)
   *
   * IRIS //Subsystem
   * @return VariationId
   */
  def fromString(variationIdString: String): VariationId = {
    variationIdString.split('.').toList match {
      case Nil              => throw new RuntimeException("") //This case will never trigger because split always returns a non-empty array
      case subsystem :: Nil => VariationId(Subsystem.withNameInsensitive(subsystem), None)
      case subsystem :: variation =>
        VariationId(Subsystem.withNameInsensitive(subsystem), Some(Variation(variation.mkString("."))))
    }
  }
}
