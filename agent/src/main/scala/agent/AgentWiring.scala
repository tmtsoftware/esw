package agent

import java.time.Duration

import agent.AgentActor.AgentState
import agent.utils.{ActorRuntime, ProcessExecutor, ProcessOutput}
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed._
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.{Await, duration}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.language.implicitConversions

class AgentWiring {
  private implicit def asFiniteDuration(d: Duration): FiniteDuration = duration.Duration.fromNanos(d.toNanos)

  private val agentConfig: Config = ConfigFactory.load().getConfig("agent")
  val agentSettings: AgentSettings = AgentSettings(
    agentConfig.getString("binariesPath"),
    agentConfig.getDuration("durationToWaitForComponentRegistration")
  )
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  lazy val actorRuntime                                             = new ActorRuntime(actorSystem)
  implicit lazy val scheduler: Scheduler                            = actorSystem.scheduler
  lazy val locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  lazy val processOutput                                            = new ProcessOutput
  lazy val processExecutor                                          = new ProcessExecutor(processOutput, agentSettings)
  lazy val agentActor                                               = new AgentActor(locationService, processExecutor, agentSettings)

  implicit private val timeout: Timeout = Timeout(10.seconds)
  lazy val agentRef: ActorRef[AgentCommand] =
    Await.result(actorSystem ? (Spawn(agentActor.behavior(AgentState.empty), "agent-actor", Props.empty, _)), timeout.duration)
}
