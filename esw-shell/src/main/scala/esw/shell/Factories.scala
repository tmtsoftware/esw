package esw.shell

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.ComponentType.{Assembly, HCD, Machine, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.akka.client.AgentClient
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerImpl
import esw.shell.utils.Extensions.FutureExt
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl

import scala.concurrent.ExecutionContext

class Factories(val locationUtils: LocationServiceUtil)(implicit val actorSystem: ActorSystem[_]) {
  implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  // ============= CSW ============
  def assemblyCommandService(prefix: String): CommandService = CommandServiceFactory.make(findAkkaLocation(prefix, Assembly))
  def hcdCommandService(prefix: String): CommandService      = CommandServiceFactory.make(findAkkaLocation(prefix, HCD))

  // ============= ESW ============
  def sequencerCommandService(subsystem: Subsystem, obsMode: String): SequencerApi =
    new SequencerImpl(findSequencer(subsystem, obsMode))
  def sequenceManager: SequenceManagerApi           = new SequenceManagerImpl(findAkkaLocation("ESW.sequence_manager", Service))
  def agentClient(agentPrefix: String): AgentClient = new AgentClient(findAkkaLocation(agentPrefix, Machine))

  // ============= INTERNAL ============
  private def findSequencer(subsystem: Subsystem, obsModeName: String): ActorRef[SequencerMsg] =
    locationUtils.findSequencer(subsystem, obsModeName).map(throwLeft(_).sequencerRef).await()

  private def findAkkaLocation(prefix: String, componentType: ComponentType): AkkaLocation =
    locationUtils.find(AkkaConnection(ComponentId(Prefix(prefix), componentType))).map(throwLeft).await()

  private def throwLeft[T](e: Either[EswLocationError, T]): T = e.fold(e => throw new RuntimeException(e.msg), identity)
}
