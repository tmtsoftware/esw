package esw.shell

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.framework.CswWiring
import csw.location.api.models.ComponentType.{Machine, Service}
import esw.agent.akka.client.AgentClient
import esw.gateway.api.AdminApi
import esw.gateway.impl.AdminImpl
import esw.shell.utils.Extensions.FutureExt
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl

class EswWiring {
  private implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = cswWiring.wiring.actorSystem

  lazy val cswWiring = new CswWiring

  private lazy val locationUtils = new LocationUtils(cswWiring.cswContext.locationService)
  lazy val commandServiceDsl     = new CommandServiceDsl(locationUtils)

  lazy val adminApi: AdminApi = new AdminImpl(cswWiring.cswContext.locationService)

  def sequenceManager(): SequenceManagerApi =
    locationUtils
      .findAkkaLocation("ESW.sequence_manager", Service)
      .map(new SequenceManagerImpl(_))(typedSystem.executionContext)
      .await()

  def agentAkkaClient(agentPrefix: String): AgentClient =
    locationUtils
      .findAkkaLocation(agentPrefix, Machine)
      .map(new AgentClient(_))(typedSystem.executionContext)
      .await()

}
