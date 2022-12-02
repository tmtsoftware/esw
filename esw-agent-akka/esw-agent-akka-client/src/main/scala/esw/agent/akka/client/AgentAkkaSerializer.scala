package esw.agent.akka.client

import csw.commons.CborAkkaSerializer
import esw.agent.akka.client.codecs.AgentActorCodecs
import esw.agent.service.api.AgentAkkaSerializable
import esw.agent.service.api.models.AgentResponse
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

// $COVERAGE-OFF$
/*
 * Serializer being used in ser(de) of agents actor messages
 */
class AgentAkkaSerializer extends CborAkkaSerializer[AgentAkkaSerializable] with AgentActorCodecs {
  override def identifier: Int = 26726

  // for some reason scala3 is not able to infer this from the original location hence moved here
  implicit lazy val agentResponseCodec: Codec[AgentResponse] = deriveAllCodecs

  register[AgentRemoteCommand]
  register[AgentResponse]
}
// $COVERAGE-ON$
