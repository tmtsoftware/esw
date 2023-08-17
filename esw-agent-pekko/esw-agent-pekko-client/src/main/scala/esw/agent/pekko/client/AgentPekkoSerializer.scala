package esw.agent.pekko.client

import csw.commons.CborPekkoSerializer
import esw.agent.pekko.client.codecs.AgentActorCodecs
import esw.agent.service.api.AgentPekkoSerializable
import esw.agent.service.api.models.AgentResponse

// $COVERAGE-OFF$
/*
 * Serializer being used in ser(de) of agents actor messages
 */
class AgentPekkoSerializer extends CborPekkoSerializer[AgentPekkoSerializable] with AgentActorCodecs {
  override def identifier: Int = 26726

  register[AgentRemoteCommand]
  register[AgentResponse]
}
// $COVERAGE-ON$
