package esw.agent.akka.client

import csw.commons.CborAkkaSerializer
import esw.agent.akka.client.codecs.AgentActorCodecs
import esw.agent.service.api.AgentAkkaSerializable
import esw.agent.service.api.models.AgentResponse

// $COVERAGE-OFF$
class AgentAkkaSerializer extends CborAkkaSerializer[AgentAkkaSerializable] with AgentActorCodecs {
  override def identifier: Int = 26726

  register[AgentRemoteCommand]
  register[AgentResponse]
}
// $COVERAGE-ON$
