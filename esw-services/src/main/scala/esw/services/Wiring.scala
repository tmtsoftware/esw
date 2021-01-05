package esw.services

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.client.ActorSystemFactory
import esw.agent.akka.app.AgentWiring
import esw.gateway.server.GatewayWiring
import esw.services.cli.Command.Start
import esw.services.internal.ManagedService
import esw.sm.app.SequenceManagerWiring

class Wiring(startCmd: Start) {
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  lazy val agentApp: ManagedService[AgentWiring] = Agent.service(startCmd.agent, startCmd.agentPrefix)
  lazy val gatewayService: ManagedService[GatewayWiring] =
    Gateway.service(startCmd.gateway, startCmd.commandRoleConfig)
  lazy val smService: ManagedService[SequenceManagerWiring] =
    SequenceManager.service(startCmd.sequenceManager, startCmd.obsModeConfig)

  lazy val serviceList = List(agentApp, gatewayService, smService)

  def start(): Unit = serviceList.foreach(_.start())

  def stop(): Unit = serviceList.foreach(_.stop())
}
