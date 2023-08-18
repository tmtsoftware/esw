package esw.agent.pekko.client

import csw.commons.CborPekkoSerializer
import esw.agent.pekko.client.codecs.AgentActorCodecs
import esw.agent.service.api.AgentPekkoSerializable
import esw.agent.service.api.models.AgentResponse
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

// $COVERAGE-OFF$
/*
 * Serializer being used in ser(de) of agents actor messages
 */
class AgentPekkoSerializer extends CborPekkoSerializer[AgentPekkoSerializable] with AgentActorCodecs {
  override def identifier: Int = 26726

  // for some reason scala3 is not able to infer this from the original location hence moved here
  implicit lazy val agentResponseCodec: Codec[AgentResponse] = deriveCodec

  register[AgentRemoteCommand]
  register[AgentResponse]
}
// $COVERAGE-ON$
