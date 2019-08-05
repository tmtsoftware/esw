package esw.ocs.core.`new`

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.messages.FSM._
import esw.ocs.api.models.messages.GoOnlineError
import esw.ocs.core.Sequencer
import esw.ocs.dsl.ScriptDsl

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class NewSequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
    script: ScriptDsl,
    locationService: LocationService
)(implicit val actorSystem: ActorSystem[_])
    extends OcsFrameworkCodecs {

  def idleBehavior: Behavior[SequencerMessage] = receive[IdleMessage]("idle") {
    ???
  }
  def loadedBehavior: Behavior[SequencerMessage] = receive[SequenceLoadedMessage]("sequence loaded") {
    ???
  }
  def inProgressBehavior: Behavior[SequencerMessage] = receive[InProgressMessage]("in-progress") {
    ???
  }
  def offlineBehavior: Behavior[SequencerMessage] = Behaviors.setup { implicit ctx =>
    import ctx.executionContext
    receive[OfflineMessage]("offline") {
      case Shutdown(replyTo) => shutdown(replyTo); Behaviors.same
      case GoOnline(replyTo) => goOnline(replyTo); Behaviors.same
    }
  }

  private def goOnline(replyTo: ActorRef[GoOnlineResponse])(implicit ec: ExecutionContext): Unit =
    sequencer.goOnline().foreach { res =>
      script.executeGoOnline() // fixme:recover and log
      res match {
        case Left(GoOnlineError(msg)) => replyTo ! EswError(msg)
        case Right(_)                 => replyTo ! EswSuccess
      }
    }

  private def shutdown(replyTo: ActorRef[ShutdownResponse])(implicit ctx: ActorContext[SequencerMessage]): Unit = {
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
  }

  private def receive[B <: SequencerMessage: ClassTag](
      stateName: String
  )(f: B => Behavior[SequencerMessage]): Behavior[SequencerMessage] =
    Behaviors.receiveMessage {
      case m: B => f(m)
      case m =>
        m.replyTo ! Unhandled(stateName, m.getClass.getSimpleName)
        Behaviors.same
    }
}
