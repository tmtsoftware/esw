package esw.agent.app.process

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models._
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand.SpawnManuallyRegistered
import esw.agent.api.Killed._
import esw.agent.api.{Failed, KillResponse, Spawned}
import esw.agent.app.AgentSettings
import esw.agent.app.process.ProcessActorMessage._

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.{Failure, Success}

class ManuallyRegisteredProcessActor[T <: Location](
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger,
    command: SpawnManuallyRegistered
) {
  import command._
  import logger._
  private val aborted         = Failed("Aborted")
  private val gracefulTimeout = agentSettings.durationToWaitForGracefulProcessTermination
  private val prefix          = registration.connection.componentId.prefix

  private def isComponentRegistered(timeout: FiniteDuration)(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService
      .resolve(registration.connection.of[T], timeout)
      .map(_.nonEmpty)

  private def registerComponent(): Future[RegistrationResult] =
    locationService.register(registration)

  private def unregisterComponent(): Future[Done] = locationService.unregister(registration.connection)

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
          warn(errorMessage, Map("prefix" -> registration.connection.componentId.prefix))
          replyTo ! Failed(errorMessage)
          Behaviors.stopped

        case RunCommand =>
          debug(s"spawning sequence component", map = Map("prefix" -> prefix))
          processExecutor.runCommand(command.commandStrings(agentSettings.binariesPath), prefix) match {
            case Right(process) =>
              process.onExit().asScala.onComplete(_ => ctx.self ! ProcessExited(process.exitValue()))
              ctx.pipeToSelf(registerComponent()) {
                case Success(registrationResult) => RegistrationSuccess
                case _                           => RegistrationFailed
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
          val errorMessage = "could not register spawned process"
          error(errorMessage, Map("pid" -> process.pid, "prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          ctx.self ! StopGracefully
          stopping(process, Set.empty)

        case ProcessExited(exitCode) =>
          val errorMessage = "process died before registration completion"
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
          unregisterComponent()
          timeScheduler.startSingleTimer(StopForcefully, gracefulTimeout)
          Behaviors.same
        case StopForcefully =>
          process.destroyForcibly()
          deathSubscribers.foreach(_ ! killedForcefully)
          Behaviors.stopped
      }
    }
}
