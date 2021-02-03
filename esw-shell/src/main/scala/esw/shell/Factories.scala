package esw.shell

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.config.api.scaladsl.ConfigService
import csw.location.api.models.ComponentType.{Assembly, HCD, Machine}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.akka.client.AgentClient
import esw.commons.extensions.EitherExt.EitherOps
import esw.commons.extensions.FutureExt.FutureOps
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts
import esw.gateway.api.AdminApi
import esw.gateway.impl.AdminImpl
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerImpl
import esw.ocs.testkit.EswTestKit
import esw.shell.component.SimulatedComponentBehaviourFactory
import esw.shell.service.SequenceManager
import esw.sm.api.SequenceManagerApi
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse

import scala.concurrent.ExecutionContext

class Factories(val locationUtils: LocationServiceUtil, configService: ConfigService)(implicit val actorSystem: ActorSystem[_]) {
  implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  private val eswTestKit: EswTestKit = new EswTestKit() {}

  // ============= CSW ============
  def assemblyCommandService(prefix: String): CommandService =
    CommandServiceFactory.make(locationUtils.findAkkaLocation(prefix, Assembly).map(_.throwLeft).await())
  def hcdCommandService(prefix: String): CommandService =
    CommandServiceFactory.make(
      locationUtils.findAkkaLocation(prefix, HCD).map(_.throwLeft).await()
    )
  def spawnSimulatedHCD(prefix: String): ActorRef[ComponentMessage] =
    eswTestKit.spawnHCD(Prefix(prefix), new SimulatedComponentBehaviourFactory()).await(CommonTimeouts.Wiring)
  def spawnSimulatedAssembly(prefix: String): ActorRef[ComponentMessage] =
    eswTestKit.spawnAssembly(Prefix(prefix), new SimulatedComponentBehaviourFactory()).await(CommonTimeouts.Wiring)

  // ============= ESW ============
  def sequencerCommandService(subsystem: Subsystem, obsMode: String): SequencerApi = {
    val sequencerRef =
      locationUtils.findSequencer(subsystem, obsMode).map(_.throwLeft).await().sequencerRef
    new SequencerImpl(sequencerRef)
  }
  def adminApi: AdminApi = new AdminImpl(locationUtils.locationService)
  def agentClient(agentPrefix: String): AgentClient =
    new AgentClient(
      locationUtils.findAkkaLocation(agentPrefix, Machine).map(_.throwLeft).await()
    )
  def sequenceManager(): SequenceManagerApi = new SequenceManager(locationUtils, configService).service
  def provision(config: ProvisionConfig, sequencerScriptsVersion: String): ProvisionResponse =
    new SequenceManager(locationUtils, configService).provision(config, sequencerScriptsVersion)
}
