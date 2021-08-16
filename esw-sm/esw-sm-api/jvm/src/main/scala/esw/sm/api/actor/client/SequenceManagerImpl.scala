package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.constants.SequenceManagerTimeouts
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol._

import scala.concurrent.Future

/**
 * Akka actor client for the sequence manager
 *
 * @param location - akka Location of the sequence manager
 * @param actorSystem - an Akka ActorSystem
 */
class SequenceManagerImpl(location: AkkaLocation)(implicit actorSystem: ActorSystem[_]) extends SequenceManagerApi {

  private val smRef: ActorRef[SequenceManagerMsg] = location.uri.toActorRef.unsafeUpcast[SequenceManagerMsg]

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] =
    (smRef ? (Configure(obsMode, _)))(SequenceManagerTimeouts.Configure, actorSystem.scheduler)

  override def provision(config: ProvisionConfig): Future[ProvisionResponse] =
    (smRef ? (Provision(config, _)))(SequenceManagerTimeouts.Provision, actorSystem.scheduler)

  override def getObsModesDetails: Future[ObsModesDetailsResponse] =
    (smRef ? GetObsModesDetails)(SequenceManagerTimeouts.GetObsModesDetails, actorSystem.scheduler)

  override def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    (smRef ? { x: ActorRef[StartSequencerResponse] => StartSequencer(subsystem, obsMode, x) })(
      SequenceManagerTimeouts.StartSequencer,
      actorSystem.scheduler
    )

  override def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    (smRef ? { x: ActorRef[RestartSequencerResponse] => RestartSequencer(subsystem, obsMode, x) })(
      SequenceManagerTimeouts.RestartSequencer,
      actorSystem.scheduler
    )

  override def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownSequencer(subsystem, obsMode, _)))(SequenceManagerTimeouts.ShutdownSequencer, actorSystem.scheduler)

  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownSubsystemSequencers(subsystem, _)))(SequenceManagerTimeouts.ShutdownSequencer, actorSystem.scheduler)

  override def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownObsModeSequencers(obsMode, _)))(SequenceManagerTimeouts.ShutdownSequencer, actorSystem.scheduler)

  override def shutdownAllSequencers(): Future[ShutdownSequencersResponse] =
    (smRef ? ShutdownAllSequencers)(SequenceManagerTimeouts.ShutdownSequencer, actorSystem.scheduler)

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    (smRef ? (ShutdownSequenceComponent(prefix, _)))(SequenceManagerTimeouts.ShutdownSequenceComponent, actorSystem.scheduler)

  override def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    (smRef ? ShutdownAllSequenceComponents)(SequenceManagerTimeouts.ShutdownSequenceComponent, actorSystem.scheduler)

  override def getResources: Future[ResourcesStatusResponse] =
    (smRef ? GetResources)(SequenceManagerTimeouts.GetResources, actorSystem.scheduler)
}
