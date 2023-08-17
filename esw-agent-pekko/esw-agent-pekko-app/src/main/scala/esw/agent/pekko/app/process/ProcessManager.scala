package esw.agent.pekko.app.process

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import csw.location.api.models.*
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import esw.agent.pekko.app.AgentSettings
import esw.agent.pekko.app.ext.ProcessExt.ProcessOps
import esw.agent.pekko.app.ext.SpawnCommandExt.SpawnCommandOps
import esw.agent.pekko.client.AgentCommand.SpawnCommand
import esw.agent.service.api.models.{Failed, KillResponse, Killed}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.config.{FetchingScriptVersionFailed, VersionManager}
import esw.constants.AgentTimeouts

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal

/**
 * This class provides methods to manage various processes that will be spawned/killed using Agent App.
 */
class ProcessManager(
    locationService: LocationService,
    versionManager: VersionManager,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings
)(implicit system: ActorSystem[_], log: Logger) {
  import system.executionContext

  def spawn(command: SpawnCommand): Future[Either[String, Process]] =
    verifyComponentIsNotAlreadyRegistered(command.connection)
      .flatMapE(_ => startComponent(command))
      .flatMapE(process =>
        waitForRegistration(command.connection, AgentTimeouts.DurationToWaitForComponentRegistration).flatMapE(_ =>
          reconcile(process, command.connection)
        )
      )

  // un-registration is done as a part of process.onComplete callback
  def kill(location: Location): Future[KillResponse] =
    getProcessHandle(location) match {
      case Left(e) => Future.successful(Failed(e))
      case Right(process) =>
        process.kill(10.seconds).map(_ => Killed).recover { case NonFatal(e) =>
          Failed(s"Failed to kill component process, reason: ${e.getMessage}".tap(log.warn(_)))
        }
    }

  // it creates a process handle with pid extracted from the metadata of the given location
  // and returns the processHandle
  // if there is no pid in the location's metadata it returns the error
  private def getProcessHandle(location: Location): Either[String, ProcessHandle] =
    location.metadata.getPid.toRight(s"$location metadata does not contain Pid").flatMap(parsePid)

  private def parsePid(pid: Long): Either[String, ProcessHandle] =
    Try(processHandle(pid)).toEither.left
      .map(_.getMessage)
      .flatMap(_.toRight(s"Pid:$pid process does not exist"))

  def processHandle(pid: Long): Option[ProcessHandle] = ProcessHandle.of(pid).toScala

  // It checks if the component of the given connection is already registered in the location service
  // If it is registered then it returns an error message string as a Future
  // otherwise it return an unit value as a Future
  private def verifyComponentIsNotAlreadyRegistered(connection: Connection): Future[Either[String, Unit]] =
    locationService
      .find(connection.of[Location])
      .map {
        case None    => Right(())
        case Some(l) => Left(s"${connection.componentId} is already registered with location service at $l".tap(log.error(_)))
      }
      .mapError(e => s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_)))

  // starts a process with the executable string of the given spawn command
  private def startComponent(command: SpawnCommand) =
    command
      .executableCommandStr(agentSettings.coursierChannel, agentSettings.prefix, versionManager)
      .map { cmdStr =>
        processExecutor
          .runCommand(cmdStr, command.prefix)
          .map(_.tap(onProcessExit(_, command.connection)))
      }
      .recover { case FetchingScriptVersionFailed(msg) =>
        Left(msg)
      }

  // it checks if the given process is alive
  // if not it tries to unregister the component of the given connection
  // and returns the error message as a Future
  // in case process is still alive it just returns process as a Future
  private def reconcile(process: Process, connection: Connection): Future[Either[String, Process]] =
    if (!process.isAlive)
      unregisterComponent(connection).transform(_ =>
        Try(Left("Process terminated before registration was successful".tap(log.warn(_))))
      )
    else Future.successful(Right(process))

  // it checks if the component of the given connection is registered in the location service within the given timeout
  // if not it returns the error message as a Future
  // otherwise it just returns unit as a Future
  private def waitForRegistration(connection: Connection, timeout: FiniteDuration): Future[Either[String, Unit]] =
    locationService
      .resolve(connection.of[Location], timeout)
      .map {
        case Some(_) => Right(())
        case None =>
          Left(
            s"${connection.componentId} is not registered with location service. Reason: Process failed to spawn due to reasons like invalid binary version etc or failed to register with location service."
              .tap(log.warn(_))
          )
      }
      .mapError(e => s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_)))

  // it attaches a job to unregister the started component on the completion of the process
  private def onProcessExit(process: Process, connection: Connection): Unit =
    process.toHandle.onComplete { _ =>
      log.warn(s"Process exited with exit value: ${process.exitValue()}, unregistering ${connection.componentId}")
      unregisterComponent(connection)
    }

  private def unregisterComponent(connection: Connection): Future[Done] = locationService.unregister(connection)
}
