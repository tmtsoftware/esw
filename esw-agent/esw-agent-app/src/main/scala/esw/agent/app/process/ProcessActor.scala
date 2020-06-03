package esw.agent.app.process

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand.SpawnCommand
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.AgentCommand.SpawnCommand.{SpawnManuallyRegistered, SpawnSelfRegistered}
import esw.agent.api.ComponentStatus.{Initializing, Running, Stopping}
import esw.agent.api.{Failed, KillResponse, Killed, Spawned}
import esw.agent.app.AgentSettings
import esw.agent.app.ext.ProcessExt.ProcessOps
import esw.agent.app.process.ProcessActorMessage._
import esw.agent.app.process.cs.Coursier
import esw.agent.app.process.redis.Redis

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ProcessActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger,
    command: SpawnCommand
) {
  import agentSettings._
  import command._
  import logger._

  private val executableCommand: List[String] = command match {
    case SpawnSequenceComponent(_, _, version, javaOpts) =>
      Coursier.ocsApp(version).launch(coursierChannel, commandArgs, javaOpts)
    case _: SpawnRedis => Redis.server :: commandArgs
  }
  private val aborted = Failed("Aborted")

  private def isComponentRegistered(timeout: FiniteDuration)(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService.resolve(connection.of[Location], timeout).map(_.nonEmpty)

  private def addUnRegistrationHook(result: RegistrationResult)(implicit system: ActorSystem[_]): Unit =
    CoordinatedShutdown(system)
      .addTask(PhaseBeforeServiceUnbind, s"unregister-${prefix.componentName}") { () =>
        result.unregister()
      }

  private def registerComponent(registration: Registration)(implicit system: ActorSystem[_]): Future[Boolean] = {
    import system.executionContext
    locationService.register(registration).map { r => addUnRegistrationHook(r); true }
  }

  private def unregisterComponent(): Future[Done] = locationService.unregister(connection)

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
    Behaviors.setup[ProcessActorMessage] { ctx =>
      import ctx.executionContext
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case AlreadyRegistered =>
          val errorMessage = "can not spawn component when it is already registered in location service"
          warn(errorMessage, Map("prefix" -> prefix))
          replyTo ! Failed(errorMessage)
          Behaviors.stopped

        case RunCommand =>
          debug(s"spawning sequence component", map = Map("prefix" -> prefix))
          processExecutor.runCommand(executableCommand, prefix) match {
            case Right(process) =>
              process.onComplete(_ => ctx.self ! ProcessExited(process.exitValue()))
              val isRegistered: Future[Boolean] =
                command match {
                  case _: SpawnSelfRegistered       => isComponentRegistered(durationToWaitForComponentRegistration)
                  case cmd: SpawnManuallyRegistered => registerComponent(cmd.registration)(ctx.system)
                }

              ctx.pipeToSelf(isRegistered) {
                case Success(true) => RegistrationSuccess
                case _             => RegistrationFailed
              }
              waitingForComponentRegistration(process, processExited = false)
            case Left(err) =>
              replyTo ! Failed(err)
              Behaviors.stopped
          }

        case LocationServiceError(exception) =>
          replyTo ! Failed(exception.getMessage)
          error(exception.getMessage, Map("prefix" -> prefix), exception)
          Behaviors.stopped

        case Die(dieRef) =>
          warn("Killing process actor via Die message. Process was not started yet", Map("prefix" -> prefix))
          dieRef ! Killed
          replyTo ! aborted
          Behaviors.stopped

        case GetStatus(replyTo) =>
          replyTo ! Initializing
          Behaviors.same
      }
    }

  def waitingForComponentRegistration(process: Process, processExited: Boolean): Behavior[ProcessActorMessage] =
    Behaviors.setup[ProcessActorMessage] { ctx =>
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
          ctx.self ! Stop
          stopping(process, None)

        case ProcessExited(exitCode) =>
          val errorMessage = "process died before registration"
          replyTo ! Failed(errorMessage)
          error(errorMessage, Map("pid" -> process.pid, "prefix" -> prefix, "exitCode" -> exitCode))
          unregisterComponent()
          Behaviors.stopped

        case Die(deathSubscriber) =>
          warn("Killing process via Die message", Map("pid" -> process.pid, "prefix" -> prefix))
          replyTo ! aborted
          ctx.self ! Stop
          stopping(process, Some(deathSubscriber))

        case GetStatus(replyTo) =>
          replyTo ! Initializing
          Behaviors.same
      }
    }

  def registered(process: Process): Behavior[ProcessActorMessage] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial[ProcessActorMessage] {
        case ProcessExited(exitCode) =>
          info(
            s"process exited, unregistering connection $connection",
            Map("pid" -> process.pid, "prefix" -> prefix, "exitCode" -> exitCode)
          )
          unregisterComponent()
          Behaviors.stopped

        case Die(deathSubscriber) =>
          warn("attempting to kill process gracefully", Map("pid" -> process.pid, "prefix" -> prefix))
          ctx.self ! Stop
          stopping(process, Some(deathSubscriber))

        case GetStatus(replyTo) =>
          replyTo ! Running
          Behaviors.same
      }
    }

  def stopping(process: Process, deathSubscriber: Option[ActorRef[KillResponse]]): Behavior[ProcessActorMessage] =
    Behaviors.receivePartial[ProcessActorMessage] {
      case (_, Die(dieRef)) =>
        debug("stop message arrived when component was already stopping", Map("pid" -> process.pid, "prefix" -> prefix))
        dieRef ! Failed("process is already stopping")
        Behaviors.same

      case (_, ProcessExited(_)) =>
        deathSubscriber.foreach(_ ! Killed)
        Behaviors.stopped

      case (ctx, Stop) =>
        process.kill(10.seconds)(ctx.system)
        unregisterComponent()
        Behaviors.same

      case (_, GetStatus(replyTo)) =>
        replyTo ! Stopping
        Behaviors.same
    }

}
