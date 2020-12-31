package esw.services

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.client.ActorSystemFactory
import esw.agent.akka.app.AgentWiring
import esw.gateway.server.GatewayWiring
import esw.services.Command.Start
import esw.sm.app.SequenceManagerWiring

class Wiring(startCmd: Start) {
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  lazy val agentApp: ManagedService[AgentWiring] = Agent.service(startCmd.agentPrefix.nonEmpty, startCmd.agentPrefix)
  lazy val gatewayService: ManagedService[GatewayWiring] =
    Gateway.service(startCmd.commandRoleConfig.nonEmpty, startCmd.commandRoleConfig)
  lazy val smService: ManagedService[SequenceManagerWiring] =
    SequenceManager.service(startCmd.obsModeConfig.nonEmpty, startCmd.obsModeConfig)

  lazy val serviceList = List(agentApp, gatewayService, smService)

  def start(): Unit = serviceList.foreach(_.start())

  def stop(): Unit = serviceList.foreach(_.stop())
}
