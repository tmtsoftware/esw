package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
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

  override def cleanup(observingMode: ObsMode): Future[CleanupResponse] = smRef ? (Cleanup(observingMode, _))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] = smRef ? GetRunningObsModes

  override def startSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[StartSequencerResponse] =
    (smRef ? { x: ActorRef[StartSequencerResponse] => StartSequencer(subsystem, observingMode, x) })(
      SequenceManagerTimeout.StartSequencerTimeout,
      actorSystem.scheduler
    )

  override def shutdownSequencer(
      subsystem: Subsystem,
      observingMode: ObsMode
  ): Future[ShutdownSequencerResponse] =
    smRef ? (ShutdownSequencer(subsystem, observingMode, _))

  override def restartSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[RestartSequencerResponse] =
    (smRef ? { x: ActorRef[RestartSequencerResponse] => RestartSequencer(subsystem, observingMode, x) })(
      SequenceManagerTimeout.RestartSequencerTimeout,
      actorSystem.scheduler
    )

  override def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse] = smRef ? ShutdownAllSequencers

  override def spawnSequenceComponent(agent: Prefix, sequenceComponentName: String): Future[SpawnSequenceComponentResponse] =
    smRef ? (SpawnSequenceComponent(agent, sequenceComponentName, _))

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
