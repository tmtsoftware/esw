package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.commons.Timeouts
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.protocol._

import scala.concurrent.Future

class SequenceManagerImpl(location: AkkaLocation)(implicit actorSystem: ActorSystem[_]) extends SequenceManagerApi {

  implicit val timeout: Timeout = Timeouts.DefaultTimeout

  private val smRef: ActorRef[SequenceManagerMsg] = location.uri.toActorRef.unsafeUpcast[SequenceManagerMsg]

  override def configure(observingMode: String)(implicit timeout: Timeout): Future[ConfigureResponse] =
    smRef ? (Configure(observingMode, _))

  override def cleanup(observingMode: String): Future[CleanupResponse] = smRef ? (Cleanup(observingMode, _))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] = smRef ? GetRunningObsModes

  override def startSequencer(subsystem: Subsystem, observingMode: String): Future[StartSequencerResponse] =
    smRef ? (StartSequencer(subsystem, observingMode, _))

  override def shutdownSequencer(
      subsystem: Subsystem,
      observingMode: String,
      shutdownSequenceComp: Boolean = false
  ): Future[ShutdownSequencerResponse] =
    smRef ? (ShutdownSequencer(subsystem, observingMode, shutdownSequenceComp, _))

  override def restartSequencer(subsystem: Subsystem, observingMode: String): Future[RestartSequencerResponse] =
    smRef ? (RestartSequencer(subsystem, observingMode, _))

  override def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse] = smRef ? ShutdownAllSequencers
}
