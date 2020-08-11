package esw.agent.akka.client

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import csw.commons.CborAkkaSerializer
import esw.agent.akka.client.codecs.AgentActorCodecs
import esw.agent.service.api.{AgentAkkaSerializable, Response}

// $COVERAGE-OFF$
class AgentAkkaSerializer(_actorSystem: ExtendedActorSystem)
    extends CborAkkaSerializer[AgentAkkaSerializable]
    with AgentActorCodecs {
  override def identifier: Int = 26726

  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  register[AgentRemoteCommand]
  register[Response]
}
// $COVERAGE-ON$
