package esw.agent.api.codecs

import esw.agent.api.AgentNotFoundException
import esw.agent.api.protocol.AgentPostRequest
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import msocket.api.ErrorProtocol

object AgentHttpCodecs extends AgentHttpCodecs

trait AgentHttpCodecs extends AgentCodecs {

  lazy implicit val agentHttpMessageCodecs: Codec[AgentPostRequest] = deriveAllCodecs

  lazy implicit val agentExceptionCodec: Codec[AgentNotFoundException] = deriveCodec

  implicit lazy val agentHttpErrorProtocol: ErrorProtocol[AgentPostRequest] =
    ErrorProtocol.bind[AgentPostRequest, AgentNotFoundException]

}
