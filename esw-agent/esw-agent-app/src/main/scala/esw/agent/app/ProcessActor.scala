package esw.agent.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.location.api.scaladsl.LocationService
import csw.location.models._
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand.SpawnCommand
import esw.agent.api.Killed._
import esw.agent.api.{Failed, KillResponse, Spawned}
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
  private val aborted         = Failed("Aborted")
  private val gracefulTimeout = agentSettings.durationToWaitForGracefulProcessTermination

  private def isComponentRegistered(timeout: FiniteDuration)(implicit executionContext: ExecutionContext): Future[Boolean] =
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
          val errorMessage = "can not spawn component when it is already registered in location service"
          warn(errorMessage, Map("prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          Behaviors.stopped

        case RunCommand =>
          debug(s"spawning sequence component", map = Map("prefix" -> prefix))
          processExecutor.runCommand(command.commandStrings(agentSettings.binariesPath), prefix) match {
            case Right(process) =>
              process.onExit().asScala.onComplete(_ => ctx.self ! ProcessExited(process.exitValue()))
              ctx.pipeToSelf(isComponentRegistered(agentSettings.durationToWaitForComponentRegistration)) {
                case Success(true) => RegistrationSuccess
                case _             => RegistrationFailed
              }
              waitingForComponentRegistration(process)
            case Left(err) =>
              replyTo ! Failed(err)
              Behaviors.stopped
          }

        case e @ LocationServiceError(exception) =>
          replyTo ! Failed(e.message)
          error(e.message, Map("prefix" -> prefix), exception)
          Behaviors.stopped

        case Die(dieRef) =>
          warn("Killing process actor via Die message. Process was not started yet", Map("prefix" -> prefix))
          dieRef ! killedGracefully
          replyTo ! aborted
          Behaviors.stopped
      }
    })

  def waitingForComponentRegistration(process: Process): Behavior[ProcessActorMessage] =
    Behaviors.setup[ProcessActorMessage](ctx => {
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case RegistrationSuccess =>
          debug(
            "spawned process is successfully registered with location service",
            Map("pid" -> process.pid, "prefix" -> prefix)
          )
          replyTo ! Spawned
          registered(process)

        case RegistrationFailed =>
          val errorMessage = "could not get registration confirmation from spawned process"
          error(errorMessage, Map("pid" -> process.pid, "prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          ctx.self ! StopGracefully
          stopping(process, Set.empty)

        case ProcessExited(exitCode) =>
          val errorMessage = "process died before registration confirmation"
          replyTo ! Failed(errorMessage)
          error(errorMessage, Map("pid" -> process.pid, "prefix" -> prefix, "exitCode" -> exitCode))
          Behaviors.stopped

        case Die(dieRef) =>
          warn("Killing process via Die message", Map("pid" -> process.pid, "prefix" -> prefix))
          replyTo ! aborted
          ctx.self ! StopGracefully
          stopping(process, Set(dieRef))
      }
    })

  def registered(process: Process): Behavior[ProcessActorMessage] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case ProcessExited(exitCode) =>
          val message = "process exited"
          info(message, Map("pid" -> process.pid, "prefix" -> prefix, "exitCode" -> exitCode))
          Behaviors.stopped

        case Die(dieRef) =>
          warn("attempting to kill process gracefully", Map("pid" -> process.pid, "prefix" -> prefix))
          ctx.self ! StopGracefully
          stopping(process, Set(dieRef))
      }
    }

  def stopping(process: Process, deathSubscribers: Set[ActorRef[KillResponse]]): Behavior[ProcessActorMessage] =
    Behaviors.withTimers { timeScheduler =>
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case Die(dieRef) =>
          stopping(process, deathSubscribers + dieRef)
        case ProcessExited(exitCode) =>
          deathSubscribers.foreach(_ ! (if (exitCode == 0 || exitCode == 143) killedGracefully else killedForcefully))
          Behaviors.stopped
        case StopGracefully =>
          process.destroy()
          timeScheduler.startSingleTimer(StopForcefully, gracefulTimeout)
          Behaviors.same
        case StopForcefully =>
          process.destroyForcibly()
          deathSubscribers.foreach(_ ! killedForcefully)
          Behaviors.stopped
      }
    }
}

object ProcessActor {
  sealed trait ProcessActorMessage
  case object SpawnComponent                       extends ProcessActorMessage
  case class Die(replyTo: ActorRef[KillResponse])  extends ProcessActorMessage
  private case object AlreadyRegistered            extends ProcessActorMessage
  private case object RunCommand                   extends ProcessActorMessage
  private case object RegistrationSuccess          extends ProcessActorMessage
  private case object RegistrationFailed           extends ProcessActorMessage
  private case object StopGracefully               extends ProcessActorMessage
  private case object StopForcefully               extends ProcessActorMessage
  private case class ProcessExited(exitCode: Long) extends ProcessActorMessage
  private case class LocationServiceError(exception: Throwable) extends ProcessActorMessage {
    val message = "error occurred while resolving a component with location service"
  }
}
