package esw.sm.api.actor.client

import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.PekkoLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.constants.SequenceManagerTimeouts
import esw.ocs.api.models.{ObsMode, Variation}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg.*
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.*

import scala.concurrent.Future

/**
 * Pekko actor client for the sequence manager
 *
 * @param location - pekko Location of the sequence manager
 * @param actorSystem - an Pekko ActorSystem
 */
class SequenceManagerImpl(location: PekkoLocation)(implicit actorSystem: ActorSystem[_]) extends SequenceManagerApi {

  private val smRef: ActorRef[SequenceManagerMsg] = location.uri.toActorRef.unsafeUpcast[SequenceManagerMsg]

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] =
    (smRef ? (Configure(obsMode, _: ActorRef[ConfigureResponse])))(SequenceManagerTimeouts.Configure, actorSystem.scheduler)

  override def provision(config: ProvisionConfig): Future[ProvisionResponse] =
    (smRef ? (Provision(config, _: ActorRef[ProvisionResponse])))(SequenceManagerTimeouts.Provision, actorSystem.scheduler)

  override def getObsModesDetails: Future[ObsModesDetailsResponse] =
    (smRef ? GetObsModesDetails.apply)(SequenceManagerTimeouts.GetObsModesDetails, actorSystem.scheduler)

  override def startSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[StartSequencerResponse] =
    (smRef ? { (x: ActorRef[StartSequencerResponse]) => StartSequencer(subsystem, obsMode, variation, x) })(
      SequenceManagerTimeouts.StartSequencer,
      actorSystem.scheduler
    )

  override def restartSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[RestartSequencerResponse] =
    (smRef ? { (x: ActorRef[RestartSequencerResponse]) => RestartSequencer(subsystem, obsMode, variation, x) })(
      SequenceManagerTimeouts.RestartSequencer,
      actorSystem.scheduler
    )

  override def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownSequencer(subsystem, obsMode, variation, _: ActorRef[ShutdownSequencersResponse])))(
      SequenceManagerTimeouts.ShutdownSequencer,
      actorSystem.scheduler
    )

  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownSubsystemSequencers(subsystem, _: ActorRef[ShutdownSequencersResponse])))(
      SequenceManagerTimeouts.ShutdownSequencer,
      actorSystem.scheduler
    )

  override def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownObsModeSequencers(obsMode, _: ActorRef[ShutdownSequencersResponse])))(
      SequenceManagerTimeouts.ShutdownSequencer,
      actorSystem.scheduler
    )

  override def shutdownAllSequencers(): Future[ShutdownSequencersResponse] =
    (smRef ? ShutdownAllSequencers.apply)(SequenceManagerTimeouts.ShutdownSequencer, actorSystem.scheduler)

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    (smRef ? (ShutdownSequenceComponent(prefix, _: ActorRef[ShutdownSequenceComponentResponse])))(
      SequenceManagerTimeouts.ShutdownSequenceComponent,
      actorSystem.scheduler
    )

  override def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    (smRef ? ShutdownAllSequenceComponents.apply)(SequenceManagerTimeouts.ShutdownSequenceComponent, actorSystem.scheduler)

  override def getResources: Future[ResourcesStatusResponse] =
    (smRef ? GetResources.apply)(SequenceManagerTimeouts.GetResources, actorSystem.scheduler)
}
