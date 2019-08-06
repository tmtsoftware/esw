package esw.ocs.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.messages.EditorError.NotAllowedOnFinishedSeq
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.SequencerResponses.{EditorResponse, LifecycleResponse, StepListResponse}
import esw.ocs.api.models.messages.{AbortError, ShutdownError}
import esw.ocs.dsl.ScriptDsl
import esw.ocs.utils.FutureEitherExt._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class SequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
    script: ScriptDsl,
    locationService: LocationService
)(implicit val actorSystem: ActorSystem[_])
    extends OcsFrameworkCodecs {

  private val atMost = 5.seconds

  //BEHAVIOURS
  def idle: Behavior[EswSequencerMessage] = receive[IdleMessage]("idle") { (ctx, msg) =>
    import ctx._
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)  => shutdown(replyTo)(ctx); Behaviors.same
      case GoOffline(replyTo) => goOffline(replyTo)

      // ===== External Editor =====
      case LoadSequence(sequence, replyTo) => sequencer.load(sequence).foreach(replyTo.tell); Behaviors.same;
      case LoadAndStart(sequence, replyTo) => sequencer.loadAndStart(sequence).foreach(replyTo.tell); Behaviors.same
      case Available(replyTo)              => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo) => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo) => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
    }
  }

  def loaded: Behavior[EswSequencerMessage] = receive[SequenceLoadedMessage]("loaded") { (ctx, msg) =>
    import ctx._
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)  => shutdown(replyTo)(ctx); Behaviors.stopped
      case GoOffline(replyTo) => goOffline(replyTo)
      case StartSequence(replyTo) => sequencer.start().foreach(replyTo.tell); Behaviors.same

      // ===== External Editor =====
      case Abort(replyTo)                 => abort(replyTo); idle
      case Available(replyTo)             => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetSequence(replyTo)           => sequencer.getSequence.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo)   => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same
      case Add(commands, replyTo)         => sequencer.add(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Pause(replyTo)                 => sequencer.pause.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Resume(replyTo)                => sequencer.resume.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Reset(replyTo)                 => sequencer.reset().foreach(replyTo ! EditorResponse(_)); idle
      case Replace(id, commands, replyTo) => sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Prepend(commands, replyTo)     => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Delete(id, replyTo)            => sequencer.delete(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case InsertAfter(id, cmds, replyTo) => sequencer.insertAfter(id, cmds).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case AddBreakpoint(id, replyTo)     => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case RemoveBreakpoint(id, replyTo) => sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo) => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
    }
  }

  def inProgress: Behavior[EswSequencerMessage] = receive[InProgressMessage]("in-progress") { (ctx, msg) =>
    import ctx._
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

  // $COVERAGE-OFF$

  private def offlineBehavior: Behavior[EswSequencerMessage] = receive[OfflineMessage]("offline") { (context, message) =>
    import context._
    message match {
      case GoOnline(replyTo)            => goOnline().foreach(replyTo.tell); idle
      case Shutdown(replyTo)            => shutdown(replyTo)(context); Behaviors.stopped
      case GetPreviousSequence(replyTo) => replyTo ! StepListResponse(None); Behaviors.same
    }
  }

  //HANDLERS
  private def goOnline()(implicit ec: ExecutionContext): Future[LifecycleResponse] =
    sequencer.goOnline().map { res =>
      script.executeGoOnline() // recover and log
      LifecycleResponse(res)
    }

  private def goOffline(replyTo: ActorRef[LifecycleResponse]): Behavior[EswSequencerMessage] = {
    //fixme: this is a temporary blocking. this will be removed after we merge active object
    val result = Await.result(sequencer.goOffline(), atMost)
    replyTo ! LifecycleResponse(result)
    result
      .map { _ =>
        script.executeGoOffline() // recover and log
        offlineBehavior
      }
      .getOrElse(Behaviors.same)
  }

  private def shutdown(replyTo: ActorRef[LifecycleResponse])(implicit ctx: ActorContext[_]): Unit = {
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

  private def abort(replyTo: ActorRef[LifecycleResponse])(implicit ec: ExecutionContext): Unit =
    sequencer
      .reset()
      .flatMap {
        case Left(NotAllowedOnFinishedSeq) => Future.successful(Left(AbortError("Not Allowed on Finished Sequence")))
        case Right(_)                      => script.executeAbort().toEither(ex => AbortError(ex.getMessage))
      }
      .foreach(replyTo ! LifecycleResponse(_))

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

  // $COVERAGE-ON$

}
