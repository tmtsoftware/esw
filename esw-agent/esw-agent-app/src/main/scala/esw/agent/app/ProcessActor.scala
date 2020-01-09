package esw.agent.app

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models._
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand.SpawnCommand
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
    command: SpawnCommand
) {
  import command._
  import componentId._
  import logger._

  private def isComponentRegistered(
      timeout: FiniteDuration = agentSettings.durationToWaitForComponentRegistration
  )(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService
      .resolve(Connection.from(ConnectionInfo(prefix, componentType, connectionType)).of[T], timeout)
      .map(_.nonEmpty)

  def behaviour: Behavior[ProcessActorMessage] =
    Behaviors.setup[ProcessActorMessage] { ctx =>
      import ctx.executionContext
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case SpawnComponent =>
          ctx.pipeToSelf(isComponentRegistered(0.seconds)) {
            case Success(true)      => AlreadyRegistered
            case Success(false)     => RunCommand
            case Failure(exception) => LocationServiceError(exception)
          }
          Behaviors.same
        case AlreadyRegistered =>
          val errorMessage = "can not spawn component when it is already registered"
          warn(errorMessage, Map("prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          Behaviors.stopped
        case RunCommand =>
          debug(s"spawning sequence component", map = Map("prefix" -> prefix))
          processExecutor.runCommand(command.strings(agentSettings.binariesPath), prefix) match {
            case Right(pid) =>
              ctx.pipeToSelf(isComponentRegistered()) {
                case Success(true) => RegistrationSuccess(pid)
                case _             => RegistrationFailed(pid)
              }
              Behaviors.same
            case Left(err) =>
              replyTo ! err
              Behaviors.stopped
          }
        case LocationServiceError(exception) =>
          val message = "error occurred while resolving a component with location service"
          replyTo ! Failed(message)
          error(message, Map("prefix" -> prefix), exception)
          Behaviors.stopped
        case RegistrationSuccess(pid) =>
          debug("spawned process is registered with location service", Map("pid" -> pid, "prefix" -> prefix))
          replyTo ! Spawned
          Behaviors.stopped
        case RegistrationFailed(pid) =>
          val errorMessage = "could not get registration confirmation from spawned process within given time"
          error(errorMessage, Map("pid" -> pid, "prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          processExecutor.killProcess(pid)
          Behaviors.stopped
      }
    }
}
object ProcessActor {
  sealed trait ProcessActorMessage
  case object SpawnComponent                                    extends ProcessActorMessage
  private case object AlreadyRegistered                         extends ProcessActorMessage
  private case object RunCommand                                extends ProcessActorMessage
  private case class RegistrationSuccess(pid: Long)             extends ProcessActorMessage
  private case class RegistrationFailed(pid: Long)              extends ProcessActorMessage
  private case class LocationServiceError(exception: Throwable) extends ProcessActorMessage
}
