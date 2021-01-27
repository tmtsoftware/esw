package esw.services

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.app.AgentWiring
import esw.agent.service.app.AgentServiceWiring
import esw.gateway.server.GatewayWiring
import esw.services.apps.{Agent, AgentService, Gateway, SequenceManager}
import esw.services.cli.Command.Start
import esw.services.internal.ManagedService
import esw.sm.app.SequenceManagerWiring

class Wiring(startCmd: Start) {

  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private lazy val agentApp: ManagedService[AgentWiring]            = Agent.service(startCmd.agent, agentPrefix, ConfigFactory.load())
  private lazy val agentService: ManagedService[AgentServiceWiring] = AgentService.service(startCmd.agentService)
  private lazy val gatewayService: ManagedService[GatewayWiring]    = Gateway.service(startCmd.gateway, startCmd.commandRoleConfig)
  private lazy val smService: ManagedService[SequenceManagerWiring] =
    SequenceManager.service(startCmd.sequenceManager, startCmd.obsModeConfig, agentPrefixForSM, startCmd.simulation)

  lazy val serviceList = List(agentApp, agentService, gatewayService, smService)

  def start(): Unit = serviceList.foreach(_.start())

  def stop(): Unit = serviceList.foreach(_.stop())

  private def DefaultAgentPrefix: Prefix = Prefix(ESW, "primary")

  private def agentPrefix: Prefix = startCmd.agentPrefix.getOrElse(DefaultAgentPrefix)

  private def agentPrefixForSM: Option[Prefix] = if (startCmd.agent) Some(agentPrefix) else None
}
