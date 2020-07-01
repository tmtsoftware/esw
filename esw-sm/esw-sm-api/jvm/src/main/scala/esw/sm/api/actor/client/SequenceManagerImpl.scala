package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.ocs.api.actor.client.SequenceComponentApiTimeout
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.protocol._

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SequenceManagerImpl(location: AkkaLocation)(implicit actorSystem: ActorSystem[_]) extends SequenceManagerApi {

  implicit val timeout: Timeout = Timeouts.DefaultTimeout

  private val smRef: ActorRef[SequenceManagerMsg] = location.uri.toActorRef.unsafeUpcast[SequenceManagerMsg]

  override def configure(observingMode: ObsMode): Future[ConfigureResponse] =
    smRef ? (Configure(observingMode, _))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] = smRef ? GetRunningObsModes

  override def startSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[StartSequencerResponse] =
    (smRef ? { x: ActorRef[StartSequencerResponse] => StartSequencer(subsystem, observingMode, x) })(
      SequenceManagerTimeout.StartSequencerTimeout,
      actorSystem.scheduler
    )

  override def shutdownObsModeSequencers(observingMode: ObsMode): Future[ShutdownSequencerResponse] =
    shutdownSequencers(None, Some(observingMode), shutdownSequenceComp = false)
  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencerResponse] =
    shutdownSequencers(Some(subsystem), None, shutdownSequenceComp = false)
  override def shutdownAllSequencers(): Future[ShutdownSequencerResponse] =
    shutdownSequencers(None, None, shutdownSequenceComp = false)
  override def shutdownSequencer(
      subsystem: Subsystem,
      observingMode: ObsMode,
      shutdownSequenceComp: Boolean = true
  ): Future[ShutdownSequencerResponse] = shutdownSequencers(Some(subsystem), Some(observingMode), shutdownSequenceComp)

  private def shutdownSequencers(
      subsystem: Option[Subsystem],
      observingMode: Option[ObsMode],
      shutdownSequenceComp: Boolean
  ): Future[ShutdownSequencerResponse] =
    smRef ? (ShutdownSequencers(subsystem, observingMode, shutdownSequenceComp, _))

  override def restartSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[RestartSequencerResponse] =
    (smRef ? { x: ActorRef[RestartSequencerResponse] => RestartSequencer(subsystem, observingMode, x) })(
      SequenceManagerTimeout.RestartSequencerTimeout,
      actorSystem.scheduler
    )

  override def spawnSequenceComponent(machine: ComponentId, name: String): Future[SpawnSequenceComponentResponse] =
    smRef ? (SpawnSequenceComponent(machine, name, _))

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    smRef ? (ShutdownSequenceComponent(prefix, _))
}

object SequenceManagerTimeout {
  val StartSequencerTimeout: FiniteDuration =
    SequenceComponentApiTimeout.StatusTimeout +     // Lookup for subsystem idle sequence component
      SequenceComponentApiTimeout.StatusTimeout +   // lookup for ESW idle sequence component as fallback
      10.seconds +                                  // spawn sequence component using agent timeout as fallback
      SequenceComponentApiTimeout.LoadScriptTimeout // load script in seq comp to start sequencer

  val RestartSequencerTimeout: FiniteDuration = 5.seconds + // get seq comp location by asking sequencer
    SequenceComponentApiTimeout.RestartScriptTimeout
}
