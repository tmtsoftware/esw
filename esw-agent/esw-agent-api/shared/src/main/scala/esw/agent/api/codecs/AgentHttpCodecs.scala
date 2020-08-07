package esw.agent.api.codecs

import esw.agent.api.protocol.AgentPostRequest
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.ErrorProtocol
import msocket.api.models.ServiceError

object AgentHttpCodecs extends AgentHttpCodecs

trait AgentHttpCodecs extends AgentCodecs {

  lazy implicit val agentHttpMessageCodecs: Codec[AgentPostRequest] = deriveAllCodecs

  implicit lazy val agentHttpErrorProtocol: ErrorProtocol[AgentPostRequest] =
    ErrorProtocol.bind[AgentPostRequest, ServiceError]

}
