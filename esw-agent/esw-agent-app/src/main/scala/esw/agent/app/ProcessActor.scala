package esw.agent.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models._
import csw.logging.api.scaladsl.Logger
import esw.agent.api.Response
import esw.agent.api.Response.{Failed, Spawned}
import esw.agent.app.ProcessActor._
import esw.agent.app.utils.ProcessExecutor

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ProcessActor[T <: Location](
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger,
    componentId: ComponentId,
    connectionType: ConnectionType,
    replyTo: ActorRef[Response],
    strings: List[String]
) {
  import componentId._
  import logger._

  private def isRegistered(
      timeout: FiniteDuration = agentSettings.durationToWaitForComponentRegistration
  )(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService
      .resolve(Connection.from(ConnectionInfo(prefix, componentType, connectionType)).of[T], timeout)
      .map(_.nonEmpty)

  def init: Behavior[ProcessActorMessage] =
    Behaviors.setup[ProcessActorMessage](ctx => {
      import ctx.executionContext
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case SpawnProcess =>
          ctx.pipeToSelf(isRegistered(0.seconds)) {
            case Success(true)  => AlreadyRegistered
            case Success(false) => NotRegistered
            case Failure(exception) =>
              val errorMessage = "error occurred while resolving a component with location service"
              error(errorMessage, Map("prefix" -> prefix), exception)
              GenericError(errorMessage)
          }
          checkingRegistration
      }
    })

  private def checkingRegistration =
    Behaviors.setup[ProcessActorMessage] { ctx =>
      import ctx.executionContext
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case AlreadyRegistered =>
          val errorMessage = "can not spawn component when it is already registered"
          warn(errorMessage, Map("prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          Behaviors.stopped
        case NotRegistered =>
          debug(s"spawning sequence component", map = Map("prefix" -> prefix))
          processExecutor.runCommand(strings, prefix) match {
            case Right(pid) =>
              ctx.pipeToSelf(isRegistered()) {
                case Success(true) => RegistrationSuccess
                case _             => RegistrationFailed
              }
              waitingForRegistration(pid)
            case Left(err) =>
              replyTo ! err
              Behaviors.stopped
          }
        case GenericError(message) =>
          replyTo ! Failed(message)
          Behaviors.stopped
      }
    }

  private def waitingForRegistration(pid: Long) =
    Behaviors.receiveMessagePartial[ProcessActorMessage] {
      case RegistrationSuccess =>
        debug("spawned process is registered with location service", Map("pid" -> pid, "prefix" -> prefix))
        replyTo ! Spawned
        Behaviors.stopped
      case RegistrationFailed =>
        val errorMessage = "could not get registration confirmation from spawned process within given time"
        error(errorMessage, Map("pid" -> pid, "prefix" -> prefix))
        replyTo ! Failed(errorMessage)
        processExecutor.killProcess(pid)
        Behaviors.stopped
    }
}
object ProcessActor {
  sealed trait ProcessActorMessage
  case object SpawnProcess                 extends ProcessActorMessage
  case object AlreadyRegistered            extends ProcessActorMessage
  case object NotRegistered                extends ProcessActorMessage
  case object RegistrationSuccess          extends ProcessActorMessage
  case object RegistrationFailed           extends ProcessActorMessage
  case class GenericError(message: String) extends ProcessActorMessage
}
