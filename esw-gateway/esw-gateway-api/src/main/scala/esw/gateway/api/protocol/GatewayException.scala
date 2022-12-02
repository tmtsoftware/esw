package esw.gateway.api.protocol

/**
 * This is the list of domain exceptions that may be returned via various ESW Services.
 * @param msg - a hint about the cause of exception
 */
sealed abstract class GatewayException(msg: String) extends RuntimeException(msg)

case class InvalidComponent(msg: String)                                                  extends GatewayException(msg)
case class EmptyEventKeys(msg: String = "Request is missing event key")                   extends GatewayException(msg)
case class EventServerUnavailable(msg: String = "Event server is unavailable")            extends GatewayException(msg)
case class InvalidMaxFrequency(msg: String = "Max frequency should be greater than zero") extends GatewayException(msg)
case class SetAlarmSeverityFailure(msg: String)                                           extends GatewayException(msg)
