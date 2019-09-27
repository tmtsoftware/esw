package esw.sm.api

sealed trait Response

object Response {
  case object Ok                extends Response
  case class Error(msg: String) extends Response
}
