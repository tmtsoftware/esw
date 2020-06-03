package esw.ocs.testkit.utils

import java.nio.file.Paths

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Prefix
import esw.agent.app.{AgentApp, AgentSettings, AgentWiring}

import scala.util.Random

trait AgentUtils {
  implicit def actorSystem: ActorSystem[SpawnProtocol.Command]

  private var agentWiring: Option[AgentWiring] = None

  lazy val agentConf: Config = ConfigFactory
    .parseString(
      s"""
      |agent {
      |  binariesPath = "${Paths.get(getClass.getResource("/").getPath).toString}"
      |  durationToWaitForComponentRegistration = 60s
      |}
      |""".stripMargin
    )
    .withFallback(ConfigFactory.load())

  lazy val agentSettings: AgentSettings = AgentSettings.from(agentConf)

  lazy val agentPrefix: Prefix = Prefix(s"esw.machine_${Random.nextInt().abs}")

  def spawnAgent(agentSettings: AgentSettings): Unit = {
    val wiring = AgentWiring.make(agentPrefix, agentSettings, actorSystem)
    agentWiring = Some(wiring)
    AgentApp.start(agentPrefix, wiring)
  }

  def shutdownAgent(): Unit = agentWiring.foreach(_.actorRuntime.shutdown(UnknownReason))

}
