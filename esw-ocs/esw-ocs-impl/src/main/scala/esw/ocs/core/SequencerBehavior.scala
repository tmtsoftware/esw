package esw.ocs.core

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.error.StepListError.NotAllowedOnFinishedSeq
import esw.ocs.api.models.messages.error.{AbortError, GoOfflineError, GoOnlineError, ShutdownError}
import esw.ocs.api.models.messages.{EditorResponse, LifecycleResponse, StepListResponse}
import esw.ocs.dsl.ScriptDsl
import esw.ocs.utils.FutureEitherExt._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

class SequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
    script: ScriptDsl,
    locationService: LocationService
) {
  private def shutdown(replyTo: ActorRef[LifecycleResponse])(implicit ctx: ActorContext[SequencerMsg]): Unit = {
    import ctx.executionContext

    sequencer.shutdown()
    locationService
      .unregister(AkkaConnection(componentId))
      .flatMap(_ => script.executeShutdown().toEither(ex => ShutdownError(ex.getMessage)))
      .recover { case NonFatal(ex) => Left(ShutdownError(ex.getMessage)) }
      .foreach(replyTo ! LifecycleResponse(_))
    //fixme: can we avoid abruptly terminating the system?
    ctx.system.terminate
  }

  private def goOnline(replyTo: ActorRef[LifecycleResponse])(implicit ec: ExecutionContext): Unit =
    sequencer.isOnline.foreach { isOnline =>
      //fixme: replyTo sender if alreadyOnline
      if (!isOnline) {
        sequencer.goOnline().foreach { _ =>
          script.executeGoOnline().toEither(ex => GoOnlineError(ex.getMessage)).foreach(replyTo ! LifecycleResponse(_))
        }
      }
    }

  private def runOfflineHandlers()(implicit ec: ExecutionContext): Future[Either[GoOfflineError, Done]] = {
    sequencer.goOffline().flatMap {
      case Right(_) =>
        script.executeGoOffline().transform {
          case Success(res) => Success(Right(res))
          case Failure(ex)  => Success(Left(GoOfflineError(ex.getMessage)))
        }
      case error => Future.successful(error)
    }
  }

  private def goOffline(replyTo: ActorRef[LifecycleResponse])(implicit ctx: ActorContext[SequencerMsg]): Unit = {
    import ctx.executionContext

    sequencer.isOnline.flatMap { isOnline =>
      val goOfflineResponseF = if (isOnline) runOfflineHandlers() else Future.successful(Right(Done))
      goOfflineResponseF.map {
        case res @ Right(_) => replyTo ! LifecycleResponse(res); ctx.self ! ChangeBehaviorToOffline
        case res            => replyTo ! LifecycleResponse(res); ctx.self ! ChangeBehaviorToDefault
      }
    }
  }

  def defaultBehavior: Behavior[SequencerMsg] = Behaviors.receive[SequencerMsg] { (ctx, msg) =>
    import ctx.executionContext

    def abort(replyTo: ActorRef[LifecycleResponse]): Unit =
      sequencer
        .reset()
        .flatMap {
          case Left(NotAllowedOnFinishedSeq) => Future.successful(Left(AbortError("Not Allowed on Finished Sequence")))
          case Right(_)                      => script.executeAbort().toEither(ex => AbortError(ex.getMessage))
        }
        .foreach(replyTo ! LifecycleResponse(_))

    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)  => shutdown(replyTo)(ctx); Behaviors.same
      case GoOnline(replyTo)  => goOnline(replyTo); Behaviors.same // should this be removed
      case GoOffline(replyTo) => goOffline(replyTo)(ctx); intermediateBehavior
      case Abort(replyTo) => abort(replyTo); Behaviors.same

      // ===== External Editor =====
      case LoadSequence(sequence, replyTo)         => sequencer.load(sequence).foreach(replyTo.tell); Behaviors.same;
      case StartSequence(replyTo)                  => sequencer.start().foreach(replyTo.tell); Behaviors.same
      case LoadAndStartSequence(sequence, replyTo) => sequencer.loadAndStart(sequence).foreach(replyTo.tell); Behaviors.same

      case Available(replyTo)   => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetSequence(replyTo) => sequencer.getSequence.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo) =>
        sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same
      case Add(commands, replyTo) => sequencer.add(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Pause(replyTo)         => sequencer.pause.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Resume(replyTo)        => sequencer.resume.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Reset(replyTo)         => sequencer.reset().foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Replace(id, commands, replyTo) =>
        sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Prepend(commands, replyTo) => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Delete(id, replyTo)        => sequencer.delete(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case InsertAfter(id, commands, replyTo) =>
        sequencer.insertAfter(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case AddBreakpoint(id, replyTo) => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case RemoveBreakpoint(id, replyTo) =>
        sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo)              => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
      case MaybeNext(replyTo)             => sequencer.mayBeNext.foreach(replyTo.tell); Behaviors.same
      case ReadyToExecuteNext(replyTo)    => sequencer.readyToExecuteNext().foreach(replyTo.tell); Behaviors.same
      case UpdateFailure(failureResponse) => sequencer.updateFailure(failureResponse); Behaviors.same
    }
  }

  private def offlineBehavior: Behavior[SequencerMsg] = Behaviors.receive[SequencerMsg] { (ctx, msg) =>
    import ctx.executionContext

    msg match {
      case Shutdown(replyTo) => shutdown(replyTo)(ctx); Behaviors.same
      case GoOnline(replyTo) => goOnline(replyTo); defaultBehavior
      case _                 => Behaviors.same // what to do on offline behavior
    }
  }

  private def intermediateBehavior: Behavior[SequencerMsg] = Behaviors.receiveMessage[SequencerMsg] {
    case ChangeBehaviorToOffline => offlineBehavior
    case ChangeBehaviorToDefault => defaultBehavior
    case _                       => Behaviors.same
  }

}
