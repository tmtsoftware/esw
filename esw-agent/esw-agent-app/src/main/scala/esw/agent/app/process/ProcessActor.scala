package esw.agent.app.process

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.api.models._
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand.{SpawnManuallyRegistered, SpawnSelfRegistered}
import esw.agent.api.ComponentStatus.{Initializing, Running, Stopping}
import esw.agent.api.Killed._
import esw.agent.api.{Failed, KillResponse, SpawnCommand, Spawned}
import esw.agent.app.AgentSettings
import esw.agent.app.process.ProcessActorMessage._

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.{Failure, Success}

class ProcessActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger,
    command: SpawnCommand
) {
  import command._
  import logger._
  private val (prefix, componentType, connectionType, autoRegistered) = command match {
    case cmd: SpawnSelfRegistered =>
      (cmd.componentId.prefix, cmd.componentId.componentType, cmd.connectionType, true)
    case cmd: SpawnManuallyRegistered =>
      (
        cmd.registration.connection.prefix,
        cmd.registration.connection.connectionInfo.componentType,
        cmd.registration.connection.connectionType,
        false
      )
  }

  private val aborted = Failed("Aborted")

  private val gracefulTimeout = agentSettings.durationToWaitForGracefulProcessTermination

  private def isComponentRegistered(timeout: FiniteDuration)(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService
      .resolve(Connection.from(ConnectionInfo(prefix, componentType, connectionType)).of[Location], timeout)
      .map(_.nonEmpty)

  private def registerComponent(): Future[RegistrationResult] = {
    locationService.register(command.asInstanceOf[SpawnManuallyRegistered].registration)
  }

  private def unregisterComponent(): Future[Done] =
    locationService.unregister(command.asInstanceOf[SpawnManuallyRegistered].registration.connection)

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
        case GetStatus(replyTo) =>
          replyTo ! Initializing
          Behaviors.same
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
              val future =
                if (autoRegistered) isComponentRegistered(agentSettings.durationToWaitForComponentRegistration)
                else registerComponent().map(_ => true)
              ctx.pipeToSelf(future) {
                case Success(true)  => RegistrationSuccess
                case Success(false) => RegistrationFailed
                case _              => RegistrationFailed
              }
              waitingForComponentRegistration(process, processExited = false)
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
        case GetStatus(replyTo) =>
          replyTo ! Initializing
          Behaviors.same
      }
    })

  def waitingForComponentRegistration(process: Process, processExited: Boolean): Behavior[ProcessActorMessage] =
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
          val errorMessage = "registration encountered an issue or timed out"
          error(errorMessage, Map("pid" -> process.pid, "prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          ctx.self ! StopGracefully
          stopping(process, None)

        case ProcessExited(exitCode) =>
          val errorMessage = "process died before registration"
          replyTo ! Failed(errorMessage)
          error(errorMessage, Map("pid" -> process.pid, "prefix" -> prefix, "exitCode" -> exitCode))
          if (!autoRegistered) unregisterComponent()
          Behaviors.stopped

        case Die(deathSubscriber) =>
          warn("Killing process via Die message", Map("pid" -> process.pid, "prefix" -> prefix))
          replyTo ! aborted
          ctx.self ! StopGracefully
          stopping(process, Some(deathSubscriber))
        case GetStatus(replyTo) =>
          replyTo ! Initializing
          Behaviors.same
      }
    })

  def registered(process: Process): Behavior[ProcessActorMessage] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case ProcessExited(exitCode) =>
          val message = "process exited"
          info(message, Map("pid" -> process.pid, "prefix" -> prefix, "exitCode" -> exitCode))
          Behaviors.stopped

        case Die(deathSubscriber) =>
          warn("attempting to kill process gracefully", Map("pid" -> process.pid, "prefix" -> prefix))
          ctx.self ! StopGracefully
          stopping(process, Some(deathSubscriber))
        case GetStatus(replyTo) =>
          replyTo ! Running
          Behaviors.same
      }
    }

  def stopping(process: Process, deathSubscriber: Option[ActorRef[KillResponse]]): Behavior[ProcessActorMessage] =
    Behaviors.withTimers { timeScheduler =>
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case Die(dieRef) =>
          debug("stop message arrived when component was already stopping", Map("pid" -> process.pid, "prefix" -> prefix))
          dieRef ! Failed("process is already stopping")
          Behaviors.same
        case ProcessExited(exitCode) =>
          deathSubscriber.foreach(_ ! (if (exitCode == 0 || exitCode == 143) killedGracefully else killedForcefully))
          Behaviors.stopped
        case StopGracefully =>
          process.destroy()
          if (!autoRegistered) unregisterComponent()
          timeScheduler.startSingleTimer(StopForcefully, gracefulTimeout)
          Behaviors.same
        case StopForcefully =>
          process.destroyForcibly()
          deathSubscriber.foreach(_ ! killedForcefully)
          Behaviors.stopped
        case GetStatus(replyTo) =>
          replyTo ! Stopping
          Behaviors.same
      }
    }
}
