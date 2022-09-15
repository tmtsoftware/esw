/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.akka.client

import csw.commons.CborAkkaSerializer
import esw.agent.akka.client.codecs.AgentActorCodecs
import esw.agent.service.api.AgentAkkaSerializable
import esw.agent.service.api.models.AgentResponse

// $COVERAGE-OFF$
/*
 * Serializer being used in ser(de) of agents actor messages
 */
class AgentAkkaSerializer extends CborAkkaSerializer[AgentAkkaSerializable] with AgentActorCodecs {
  override def identifier: Int = 26726

  register[AgentRemoteCommand]
  register[AgentResponse]
}
// $COVERAGE-ON$
