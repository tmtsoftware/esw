package esw.commons.utils.location

import java.util.concurrent.CompletionStage

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.client.AgentClient
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class ComponentFactory(locationServiceUtil: LocationServiceUtil)(
    implicit val actorSystem: ActorSystem[_]
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext
  implicit val timeout: Timeout     = Timeouts.DefaultTimeout

  def resolveComponentRef(
      prefix: Prefix,
      componentType: ComponentType
  ): Future[Either[EswLocationError, ActorRef[ComponentMessage]]] =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(prefix, componentType)))
      .mapRight(_.componentRef)

  def resolveSequencer(
      subsystem: Subsystem,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): Future[Either[EswLocationError, SequencerApi]] =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(Prefix(subsystem, observingMode), Sequencer)), timeout)
      .mapRight(SequencerApiFactory.make)

  def findAgent(subsystem: Subsystem): Future[Either[EswLocationError, AgentClient]] =
    locationServiceUtil
      .listAkkaLocationsBy(subsystem, Machine)
      .flatMapRight { locations =>
        // Exception thrown by getAgentClient is handled in mapError block
        getAgentClient(locations)
      }
      .mapError(_ => ResolveLocationFailed(s"Could not find agent matching $subsystem"))

  // AgentClient.make or locations.head.prefix can throw exceptions
  // These will be handled in public method composing over getAgentClient
  private[commons] def getAgentClient(locations: List[AkkaLocation]): Future[AgentClient] =
    AgentClient.make(locations.head.prefix, locationServiceUtil.locationService)

  def resolveSeqComp(seqCompPrefix: Prefix): Future[Either[EswLocationError, SequenceComponentApi]] =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)))
      .mapRight(loc => new SequenceComponentImpl(loc))

  // Added this to be accessed by kotlin
  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    resolveComponentRef(prefix, componentType).toJava
}
