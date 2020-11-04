package esw.ocs.testkit.utils

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}

import scala.util.Random

trait AgentUtils {
  implicit def actorSystem: ActorSystem[SpawnProtocol.Command]

  private var agentWiring: Option[AgentWiring] = None
  lazy val agentSettings: AgentSettings        = AgentSettings(getRandomAgentPrefix(ESW), ConfigFactory.load())

  def getRandomAgentPrefix(subsystem: Subsystem): Prefix = Prefix(subsystem, s"machine_${Random.nextInt().abs}")

  def spawnAgent(agentSettings: AgentSettings): Unit = {
    val system = actorSystem
    val wiring = new AgentWiring(agentSettings) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = system
    }
    agentWiring = Some(wiring)
    AgentApp.start(wiring, startLogging = false)
  }

  def shutdownAgent(): Unit = agentWiring.foreach(_.actorRuntime.shutdown(UnknownReason))
}
