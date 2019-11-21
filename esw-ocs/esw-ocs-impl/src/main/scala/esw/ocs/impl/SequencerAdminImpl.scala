package esw.ocs.impl

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Source}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.models.{SequencerInsight, StepList}
import esw.ocs.api.protocol._
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState.{Idle, Offline}
import msocket.api.models.Subscription

import scala.concurrent.{ExecutionContext, Future}

class SequencerAdminImpl(sequencer: ActorRef[SequencerMsg], insightSource: Source[SequencerInsight, NotUsed])(
    implicit system: ActorSystem[_],
    timeout: Timeout
) extends SequencerAdminApi {
  private implicit val ec: ExecutionContext = system.executionContext

  override def getSequence: Future[Option[StepList]] = sequencer ? GetSequence

  override def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]       = sequencer ? (Add(commands, _))
  override def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]   = sequencer ? (Prepend(commands, _))
  override def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = sequencer ? (Replace(id, commands, _))

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] =
    sequencer ? (InsertAfter(id, commands, _))

  override def delete(id: Id): Future[GenericResponse]                    = sequencer ? (Delete(id, _))
  override def pause: Future[PauseResponse]                               = sequencer ? Pause
  override def resume: Future[OkOrUnhandledResponse]                      = sequencer ? Resume
  override def addBreakpoint(id: Id): Future[GenericResponse]             = sequencer ? (AddBreakpoint(id, _))
  override def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] = sequencer ? (RemoveBreakpoint(id, _))
  override def reset(): Future[OkOrUnhandledResponse]                     = sequencer ? Reset
  override def abortSequence(): Future[OkOrUnhandledResponse]             = sequencer ? AbortSequence
  override def stop(): Future[OkOrUnhandledResponse]                      = sequencer ? Stop

  override def isAvailable: Future[Boolean] = getState.map(_ == Idle)

  private def getState: Future[SequencerState[SequencerMsg]] = sequencer ? GetSequencerState

  override def isOnline: Future[Boolean] = getState.map(_ != Offline)

  override def getInsights: Source[SequencerInsight, Subscription] =
    insightSource
      .viaMat(KillSwitches.single)(Keep.right)
      .mapMaterializedValue(ks => () => ks.shutdown())
}
