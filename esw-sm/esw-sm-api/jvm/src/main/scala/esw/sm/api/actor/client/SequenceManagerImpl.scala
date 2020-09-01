package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.{Timeouts => CommonsTimeout}
import esw.constants.Timeouts
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol._

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SequenceManagerImpl(location: AkkaLocation)(implicit actorSystem: ActorSystem[_]) extends SequenceManagerApi {

  implicit val timeout: Timeout = CommonsTimeout.DefaultTimeout

  private val smRef: ActorRef[SequenceManagerMsg] = location.uri.toActorRef.unsafeUpcast[SequenceManagerMsg]

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] =
    smRef ? (Configure(obsMode, _))

  override def provision(config: ProvisionConfig): Future[ProvisionResponse] = smRef ? (Provision(config, _))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] = smRef ? GetRunningObsModes

  override def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    (smRef ? { x: ActorRef[StartSequencerResponse] => StartSequencer(subsystem, obsMode, x) })(
      SequenceManagerTimeout.StartSequencerTimeout,
      actorSystem.scheduler
    )

  override def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    (smRef ? { x: ActorRef[RestartSequencerResponse] => RestartSequencer(subsystem, obsMode, x) })(
      SequenceManagerTimeout.RestartSequencerTimeout,
      actorSystem.scheduler
    )

  override def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    smRef ? (ShutdownSequencer(subsystem, obsMode, _))

  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    smRef ? (ShutdownSubsystemSequencers(subsystem, _))

  override def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    smRef ? (ShutdownObsModeSequencers(obsMode, _))

  override def shutdownAllSequencers(): Future[ShutdownSequencersResponse] =
    smRef ? ShutdownAllSequencers

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    smRef ? (ShutdownSequenceComponent(prefix, _))

  override def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    smRef ? ShutdownAllSequenceComponents

  override def getAgentStatus: Future[AgentStatusResponse] = smRef ? GetAllAgentStatus
}

object SequenceManagerTimeout {
  val StartSequencerTimeout: FiniteDuration =
    Timeouts.StatusTimeout +     // Lookup for subsystem idle sequence component
      Timeouts.StatusTimeout +   // lookup for ESW idle sequence component as fallback
      10.seconds +                                  // spawn sequence component using agent timeout as fallback
      Timeouts.LoadScriptTimeout // load script in seq comp to start sequencer

  val RestartSequencerTimeout: FiniteDuration = 5.seconds + // get seq comp location by asking sequencer
    Timeouts.RestartScriptTimeout
}
