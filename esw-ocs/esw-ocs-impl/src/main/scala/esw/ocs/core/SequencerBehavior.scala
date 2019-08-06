package esw.ocs.core

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.Sequence
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.messages.EditorError.{NotAllowedOnFinishedSeq, ResetError}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.SequencerResponses.{EditorResponse, LifecycleResponse, LoadSequenceResponse, StepListResponse}
import esw.ocs.api.models.messages.{EditorError, ShutdownError}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.dsl.ScriptDsl

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.NonFatal

//todo: make steplist option
case class SequencerState(
    stepList: StepList,
    previousStepList: Option[StepList],
    readyToExecuteNextPromise: Option[Promise[Done]],
    stepRefPromise: Option[Promise[Step]]
)
object SequencerState {
  def initial = SequencerState(StepList.empty, None, None, None)
}

class SequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
    script: ScriptDsl,
    locationService: LocationService
)(implicit val actorSystem: ActorSystem[_])
    extends OcsFrameworkCodecs {

  private val emptyChildId = Id("empty-child") // fixme

  private val atMost = 5.seconds

  //BEHAVIOURS
  def idle(state: SequencerState): Behavior[EswSequencerMessage] = receive[IdleMessage]("idle") { (ctx, msg) =>
    import ctx._
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)  => shutdown(replyTo)(ctx)
      case GoOffline(replyTo) => goOffline(replyTo)

      // ===== External Editor =====
      case LoadSequence(sequence, replyTo) => load(sequence, replyTo, state)
      case LoadAndStart(sequence, replyTo) => sequencer.loadAndStart(sequence).foreach(replyTo.tell); Behaviors.same
      case Available(replyTo)              => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo) => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo) => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
    }
  }

  def loaded(state: SequencerState): Behavior[EswSequencerMessage] = receive[SequenceLoadedMessage]("loaded") { (ctx, msg) =>
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)  => shutdown(replyTo)(ctx); Behaviors.stopped
      case GoOffline(replyTo) => goOffline(replyTo)
      case StartSequence(replyTo) => sequencer.start().foreach(replyTo.tell); Behaviors.same

      // ===== External Editor =====
      case Abort(replyTo)               => abort(replyTo); idle
      case Available(replyTo)           => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetSequence(replyTo)         => sequencer.getSequence.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo) => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same
      case Add(commands, replyTo)       => sequencer.add(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Pause(replyTo)               => sequencer.pause.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Resume(replyTo)              => sequencer.resume.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Reset(replyTo)               => sequencer.reset().foreach(replyTo ! EditorResponse(_)); idle
      case Replace(id, commands, replyTo) =>
        sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Prepend(commands, replyTo) => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Delete(id, replyTo)        => sequencer.delete(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case InsertAfter(id, cmds, replyTo) =>
        sequencer.insertAfter(id, cmds).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case AddBreakpoint(id, replyTo) => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case RemoveBreakpoint(id, replyTo) => sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo) => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
    }
  }

  def inProgress(state: SequencerState): Behavior[EswSequencerMessage] = receive[InProgressMessage]("in-progress") { (ctx, msg) =>
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo) => shutdown(replyTo)(ctx); Behaviors.same
      case Abort(replyTo) => abort(replyTo); Behaviors.same

      // ===== External Editor =====
      case Available(replyTo)             => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetSequence(replyTo)           => sequencer.getSequence.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo)   => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same
      case Add(commands, replyTo)         => sequencer.add(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Pause(replyTo)                 => sequencer.pause.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Resume(replyTo)                => sequencer.resume.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Reset(replyTo)                 => sequencer.reset().foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Replace(id, commands, replyTo) => sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Prepend(commands, replyTo)     => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Delete(id, replyTo)            => sequencer.delete(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case InsertAfter(id, cmds, replyTo) => sequencer.insertAfter(id, cmds).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case AddBreakpoint(id, replyTo)     => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case RemoveBreakpoint(id, replyTo) => sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo)              => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
      case MaybeNext(replyTo)             => sequencer.mayBeNext.foreach(replyTo.tell); Behaviors.same
      case ReadyToExecuteNext(replyTo)    => sequencer.readyToExecuteNext().foreach(replyTo.tell); Behaviors.same
      case UpdateFailure(failureResponse) => sequencer.updateFailure(failureResponse); Behaviors.same
    }
  }

  def offline(state: SequencerState): Behavior[EswSequencerMessage] = receive[OfflineMessage]("offline") { (context, message) =>
    message match {
      case GoOnline(replyTo)            => goOnline().foreach(replyTo.tell); idle
      case Shutdown(replyTo)            => shutdown(replyTo)(context); Behaviors.stopped
      case GetPreviousSequence(replyTo) => replyTo ! StepListResponse(None); Behaviors.same
    }
  }

  // $COVERAGE-OFF$

  def load(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse], state: SequencerState): Behavior[EswSequencerMessage] = {
    StepList(sequence) match {
      case Left(err) =>
        replyTo ! LoadSequenceResponse(Left(err))
        Behaviors.same
      case Right(x) =>
        replyTo ! LoadSequenceResponse(Right(Done))
        loaded(state.copy(stepList = x))
    }
  }

  private def shutdown(replyTo: ActorRef[LifecycleResponse])(
      implicit ctx: ActorContext[_]
  ): Behavior[EswSequencerMessage] = {
    //todo: this blocking is temporary and will go away when shutdown story is played
    Try {
      Await.result(
        locationService
          .unregister(AkkaConnection(componentId)),
        atMost
      )
      Try {
        Await.result(script.executeShutdown(), atMost) //todo: log this
      }
      replyTo ! LifecycleResponse(Right(Done))
      //fixme : this is not safe. not sure of previous message is sent yet.
      // to be looked at in shutdown story
      ctx.system.terminate
      Behaviors.stopped[EswSequencerMessage]
    }.recover {
      case NonFatal(err) =>
        replyTo ! LifecycleResponse(
          Left(
            ShutdownError(
              "could not unregister sequencer\n" +
                err.getMessage
            )
          )
        )
        Behaviors.same[EswSequencerMessage]
    }.get
  }

  private def abort(replyTo: ActorRef[LifecycleResponse])(implicit ec: ExecutionContext): Unit =
      .reset()
      .flatMap {
        case Left(NotAllowedOnFinishedSeq) => Future.successful(Left(AbortError("Not Allowed on Finished Sequence")))
        case Right(_)                      => script.executeAbort().toEither(ex => AbortError(ex.getMessage))
      }
      .foreach(replyTo ! LifecycleResponse(_))

  def reset(): Future[Either[ResetError, Done]] = updateStepListResult(stepList.discardPending)

  // stepListResultFunc is by name because all StepList operations must execute on strandEc
  private def updateStepListResult[T <: EditorError](stepListResultFunc: => Either[T, StepList]) = {
    val stepListResult = stepListResultFunc
    stepListResult.map { s =>
      stepList = s
      checkForSequenceCompletion()
      completeStepRefPromise()
      Done
    }
  }

  protected def receive[B <: EswSequencerMessage: ClassTag](
      stateName: String
  )(f: (ActorContext[B], B) => Behavior[EswSequencerMessage]): Behavior[EswSequencerMessage] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case m: B => f(ctx, m)
        case _    =>
          //fixme: handle the Unhandled
          //m.replyTo ! Unhandled(stateName, m.getClass.getSimpleName)
          Behaviors.same
      }
    }

  //HANDLERS
  private def goOnline()(implicit ec: ExecutionContext): Future[LifecycleResponse] =
    sequencer.goOnline().map { res =>
      script.executeGoOnline() // recover and log
      LifecycleResponse(res)
    }

  private def goOffline(replyTo: ActorRef[LifecycleResponse]): Behavior[EswSequencerMessage] = {
    replyTo ! LifecycleResponse(Right(Done))
    script.executeGoOffline() // recover and log
    offline
  }

  // $COVERAGE-ON$

}
