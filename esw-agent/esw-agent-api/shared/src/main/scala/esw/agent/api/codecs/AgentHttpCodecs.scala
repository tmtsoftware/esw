package esw.agent.api.codecs

import esw.agent.api.protocol.AgentHttpMessage
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

object AgentHttpCodecs extends AgentHttpCodecs
trait AgentHttpCodecs extends AgentCodecs {

  lazy implicit val agentHttpMessageCodecs: Codec[AgentHttpMessage] = deriveAllCodecs

  implicit lazy val agentHttpErrorProtocol: ErrorProtocol[AgentHttpMessage] =
    ErrorProtocol.bind[AgentHttpMessage, ServiceError]

}
