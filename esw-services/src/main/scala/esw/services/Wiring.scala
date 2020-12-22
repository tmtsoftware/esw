package esw.services

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.services.Command.Start

class Wiring(startCmd: Start) {
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  lazy val agentApp: ManagedService[AgentWiring] =
    ManagedService("agent", startCmd.agentPrefix.nonEmpty, () => startAgent(startCmd.agentPrefix), stopAgent)

  lazy val serviceList = List(agentApp)

  def start(): Unit = {
    serviceList.foreach(_.start())
  }

  def stop(): Unit = {
    serviceList.foreach(_.stop())
  }

  private def startAgent(maybePrefix: Option[String]): Option[AgentWiring] = {
    maybePrefix.map(p => AgentApp.start(AgentSettings(Prefix(p), ConfigFactory.load())))
  }

  private def stopAgent(wiring: AgentWiring): Unit = {
    wiring.actorSystem.terminate()
    wiring.actorSystem.whenTerminated
  }
}
