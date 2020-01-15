package esw.agent.app

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models._
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand.SpawnCommand
import esw.agent.api.Response
import esw.agent.api.Response.{Failed, Ok}
import esw.agent.app.ProcessActor._
import esw.agent.app.utils.ProcessExecutor

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps
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
  private val aborted = Failed("Aborted")

  private def isComponentRegistered(
      timeout: FiniteDuration = agentSettings.durationToWaitForComponentRegistration
  )(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService
      .resolve(Connection.from(ConnectionInfo(prefix, componentType, connectionType)).of[T], timeout)
      .map(_.nonEmpty)

  def init: Behavior[ProcessActorMessage] =
    Behaviors.setup[ProcessActorMessage] { ctx =>
      import ctx.executionContext
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case SpawnComponent =>
          ctx.pipeToSelf(isComponentRegistered(0.seconds)) {
            case Success(true)      => AlreadyRegistered
            case Success(false)     => RunCommand
            case Failure(exception) => LocationServiceError(exception)
          }
          checkingRegistration
      }
    }

  def checkingRegistration: Behavior[ProcessActorMessage] =
    Behaviors.setup[ProcessActorMessage](ctx => {
      import ctx.executionContext
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case AlreadyRegistered =>
          val errorMessage = "can not spawn component when it is already registered"
          warn(errorMessage, Map("prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          Behaviors.stopped

        case RunCommand =>
          debug(s"spawning sequence component", map = Map("prefix" -> prefix))
          processExecutor.runCommand(command.commandStrings(agentSettings.binariesPath), prefix) match {
            case Right(processHandle) =>
              processHandle.onExit().asScala.onComplete {
                case Failure(exception) =>
                  error("error occurred while running command", Map("pid" -> processHandle.pid, "prefix" -> prefix), exception)
                  ctx.self ! ProcessExited(-1)
                case Success(processHandle) =>
                  ctx.self ! ProcessExited(processHandle.pid())
              }
              ctx.pipeToSelf(isComponentRegistered()) {
                case Success(true) => RegistrationSuccess
                case _             => RegistrationFailed
              }
              waitingForComponentRegistration(processHandle)
            case Left(err) =>
              replyTo ! Failed(err)
              Behaviors.stopped
          }

        case LocationServiceError(exception) =>
          val message = "error occurred while resolving a component with location service"
          replyTo ! Failed(message)
          error(message, Map("prefix" -> prefix), exception)
          Behaviors.stopped

        case Die(dieRef) =>
          warn("Killing process actor via Die message. Process was not started yet", Map("prefix" -> prefix))
          dieRef ! Ok
          replyTo ! aborted
          Behaviors.stopped
      }
    })

  def waitingForComponentRegistration(processHandle: ProcessHandle): Behavior[ProcessActorMessage] =
    Behaviors.receiveMessagePartial[ProcessActorMessage] {
      case RegistrationSuccess =>
        debug("spawned process is registered with location service", Map("pid" -> processHandle.pid, "prefix" -> prefix))
        replyTo ! Ok
        registered(processHandle)

      case RegistrationFailed =>
        val errorMessage = "could not get registration confirmation from spawned process within given time"
        error(errorMessage, Map("pid" -> processHandle.pid, "prefix" -> prefix))
        replyTo ! Failed(errorMessage)
        processHandle.destroyForcibly()
        Behaviors.stopped

      case ProcessExited(exitCode) =>
        val errorMessage = "process died before registration confirmation"
        replyTo ! Failed(errorMessage)
        error(errorMessage, Map("pid" -> processHandle.pid, "prefix" -> prefix, "exitCode" -> exitCode))
        Behaviors.stopped

      case Die(dieRef) =>
        warn("Killing process via Die message", Map("pid" -> processHandle.pid, "prefix" -> prefix))
        processHandle.destroyForcibly()
        dieRef ! Ok
        replyTo ! aborted
        Behaviors.stopped
    }

  def registered(processHandle: ProcessHandle): Behavior[ProcessActorMessage] = {
    Behaviors.receiveMessagePartial[ProcessActorMessage] {
      case ProcessExited(exitCode) =>
        val message                                         = "error occurred while running command"
        val logFunction: (String, Map[String, Any]) => Unit = if (exitCode == 0) info(_, _) else warn(_, _)
        logFunction(message, Map("pid" -> processHandle.pid, "prefix" -> prefix, "exitCode" -> exitCode))
        Behaviors.stopped

      case Die(dieRef) =>
        warn("Killing process via Die message", Map("pid" -> processHandle.pid, "prefix" -> prefix))
        processHandle.destroyForcibly()
        dieRef ! Ok
        Behaviors.stopped
    }
  }
}

object ProcessActor {
  sealed trait ProcessActorMessage
  case object SpawnComponent                                    extends ProcessActorMessage
  case class Die(replyTo: ActorRef[Response])                   extends ProcessActorMessage
  private case object AlreadyRegistered                         extends ProcessActorMessage
  private case object RunCommand                                extends ProcessActorMessage
  private case object RegistrationSuccess                       extends ProcessActorMessage
  private case object RegistrationFailed                        extends ProcessActorMessage
  private case class ProcessExited(exitCode: Long)              extends ProcessActorMessage
  private case class LocationServiceError(exception: Throwable) extends ProcessActorMessage
}
