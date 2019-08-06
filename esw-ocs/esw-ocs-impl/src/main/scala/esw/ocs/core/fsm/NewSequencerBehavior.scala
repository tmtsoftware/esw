package esw.ocs.core.fsm

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandResponse, Sequence}
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.messages.FSM._
import esw.ocs.api.models.messages.SequenceError._
import esw.ocs.api.models.messages.{GoOfflineError, GoOnlineError}
import esw.ocs.core.Sequencer
import esw.ocs.dsl.ScriptDsl

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class NewSequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
    script: ScriptDsl,
    locationService: LocationService
)(implicit val actorSystem: ActorSystem[_])
    extends FSM
    with OcsFrameworkCodecs {

  val atMost: FiniteDuration = 5.seconds

  def loadedBehavior: Behavior[SequencerMessage] =
    Behaviors.setup { implicit ctx =>
      import ctx._
      receive[SequenceLoadedMessage]("sequence-loaded") {
        //fixme: this blocking is temporary as once we
        // dissolve the active object, this should not return a future
        case StartSequence(replyTo) => Await.result(startSequence(replyTo), atMost)
        case Shutdown(replyTo)      => shutdown(replyTo)
        case GoOffline(replyTo)     => goOffline(replyTo)
      }
    }

  def inProgressBehavior: Behavior[SequencerMessage] = receive[InProgressMessage]("in-progress") {
    ???
  }

  def idleBehavior: Behavior[SequencerMessage] = Behaviors.setup { implicit ctx =>
    import ctx._
    receive[IdleMessage]("idle") {
      case Shutdown(replyTo)               => shutdown(replyTo)
      case GoOffline(replyTo)              => goOffline(replyTo)
      case LoadSequence(sequence, replyTo) =>
        //fixme: this blocking is temporary as once we
        // dissolve the active object, this should not return a future
        Await.result(loadSequence(sequence, replyTo), atMost)
      case LoadAndStartSequence(sequence, replyTo) =>
        //fixme: this blocking is temporary until we merge active object
        Await.result(loadAndStart(sequence, replyTo), atMost)
    }
  }

  def offlineBehavior: Behavior[SequencerMessage] = Behaviors.setup { implicit ctx =>
    import ctx.executionContext

    receive[OfflineMessage]("offline") {
      case Shutdown(replyTo) => shutdown(replyTo)
      case GoOnline(replyTo) => goOnline(replyTo)
    }
  }

  protected def shutdown(
      replyTo: ActorRef[SequencerResponse]
  )(implicit ctx: ActorContext[SequencerMessage]): Behavior[SequencerMessage] = {
    import ctx.executionContext

    sequencer.shutdown()
    locationService
      .unregister(AkkaConnection(componentId))
      .flatMap(_ => script.executeShutdown())
      .onComplete {
        case Failure(exception) => replyTo ! SequencerError(exception)
        case Success(_)         => replyTo ! SequencerSuccess
      }
    //fixme: can we avoid abruptly terminating the system?
    ctx.system.terminate
    Behaviors.stopped
  }

  private def startSequence(replyTo: ActorRef[SequencerResponse])(
      implicit executionContext: ExecutionContext
  ): Future[Behavior[SequencerMessage]] =
    sequencer
      .start()
      .map { x =>
        replyTo ! x
        inProgressBehavior
      }

  private def loadAndStart(sequence: Sequence, replyTo: ActorRef[SequencerResponse])(
      implicit ec: ExecutionContext
  ): Future[Behavior[SequencerMessage]] =
    sequencer
      .loadAndStart(sequence)
      .map(x => {
        replyTo ! x
        inProgressBehavior
      })

  implicit private def convertToSequencerResponse(submitResponse: SubmitResponse): SequencerResponse =
    if (CommandResponse.isNegative(submitResponse)) SequencerError(submitResponse)
    else SequencerResult(submitResponse)

  private def loadSequence(sequence: Sequence, replyTo: ActorRef[SequencerResponse])(
      implicit ec: ExecutionContext
  ): Future[Behavior[SequencerMessage]] = {
    // these is an assumption here and even in previous approach that these futures will not fail
    // this is an unsafe assumptions
    sequencer.load(sequence).map { x =>
      x.response match {
        case Left(error) =>
          error match {
            case x @ DuplicateIdsFound =>
              replyTo ! SequencerError(x.description)
              Behaviors.same
            case ExistingSequenceIsInProcess => ??? //redundant now
            case error: GenericError         => ??? // not required any more
          }
        case Right(_) =>
          replyTo ! SequencerSuccess
          loadedBehavior
      }
    }
  }

  private def goOffline(
      replyTo: ActorRef[SequencerResponse]
  )(implicit ctx: ActorContext[SequencerMessage]): Behavior[SequencerMessage] = {
    import ctx.executionContext
    sequencer
      .goOffline()
      .foreach {
        case Right(_) =>
          script.executeGoOffline() // recover and log
          replyTo ! SequencerSuccess
        case Left(GoOfflineError(msg)) => ???
        //this case will not occur as we plan to use dedicated states
      }
    offlineBehavior
  }

  private def goOnline(replyTo: ActorRef[SequencerResponse])(implicit ec: ExecutionContext): Behavior[SequencerMessage] = {
    sequencer.goOnline().foreach {
      case Left(GoOnlineError(msg)) => replyTo ! SequencerError(msg)
      case Right(_) =>
        script.executeGoOnline() // fixme:recover and log
        replyTo ! SequencerSuccess
    }
    idleBehavior
  }
}
