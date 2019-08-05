package esw.ocs.core.fsm

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.Sequence
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.messages.FSM._
import esw.ocs.api.models.messages.SequenceError._
import esw.ocs.api.models.messages.{GoOfflineError, GoOnlineError}
import esw.ocs.core.Sequencer
import esw.ocs.dsl.ScriptDsl

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class NewSequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
    script: ScriptDsl,
    locationService: LocationService
)(implicit val actorSystem: ActorSystem[_])
    extends FSM
    with OcsFrameworkCodecs {

  def loadedBehavior: Behavior[SequencerMessage] = receive[SequenceLoadedMessage]("sequence loaded") {
    ???
  }
  def inProgressBehavior: Behavior[SequencerMessage] = receive[InProgressMessage]("in-progress") {
    ???
  }

  protected def shutdown(
      replyTo: ActorRef[ShutdownResponse]
  )(implicit ctx: ActorContext[SequencerMessage]): Behavior[SequencerMessage] = {
    import ctx.executionContext

    sequencer.shutdown()
    locationService
      .unregister(AkkaConnection(componentId))
      .flatMap(_ => script.executeShutdown())
      .onComplete {
        case Failure(exception) => replyTo ! EswError(exception)
        case Success(_)         => replyTo ! EswSuccess
      }
    //fixme: can we avoid abruptly terminating the system?
    ctx.system.terminate
    Behaviors.stopped
  }

  def idleBehavior: Behavior[SequencerMessage] = Behaviors.setup { implicit ctx =>
    import ctx._
    receive[IdleMessage]("idle") {
      case Shutdown(replyTo)               => shutdown(replyTo)
      case GoOffline(replyTo)              => goOffline(replyTo)
      case LoadSequence(sequence, replyTo) =>
        //fixme: this blocking is temporary as once we
        // dissolve the active object, this should not return a future
        Await.result(loadSequence(sequence, replyTo), 5.seconds)
    }
  }

  def offlineBehavior: Behavior[SequencerMessage] = Behaviors.setup { implicit ctx =>
    import ctx.executionContext

    receive[OfflineMessage]("offline") {
      case Shutdown(replyTo) => shutdown(replyTo)
      case GoOnline(replyTo) => goOnline(replyTo)
    }
  }

  private def loadSequence(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse])(
      implicit ec: ExecutionContext
  ): Future[Behavior[SequencerMessage]] = {
    // these is an assumption here and even in previous approach that these futures will not fail
    // this is an unsafe assumptions
    sequencer.load(sequence).map { x =>
      x.response match {
        case Left(error) =>
          error match {
            case x @ DuplicateIdsFound =>
              replyTo ! EswError(x.description)
              Behaviors.same
            case ExistingSequenceIsInProcess => ??? //redundant now
            case error: GenericError         => ??? // not required any more
          }
        case Right(_) =>
          replyTo ! EswSuccess
          loadedBehavior
      }
    }
  }

  private def goOffline(
      replyTo: ActorRef[GoOfflineResponse]
  )(implicit ctx: ActorContext[SequencerMessage]): Behavior[SequencerMessage] = {
    import ctx.executionContext
    sequencer
      .goOffline()
      .foreach {
        case Right(_) =>
          script.executeGoOffline() // recover and log
          replyTo ! EswSuccess
        case Left(GoOfflineError(msg)) => ???
        //this case will not occur as we plan to use dedicated states
      }
    offlineBehavior
  }

  private def goOnline(replyTo: ActorRef[GoOnlineResponse])(implicit ec: ExecutionContext): Behavior[SequencerMessage] = {
    sequencer.goOnline().foreach {
      case Left(GoOnlineError(msg)) => replyTo ! EswError(msg)
      case Right(_) =>
        script.executeGoOnline() // fixme:recover and log
        replyTo ! EswSuccess
    }
    idleBehavior
  }
}
