package esw.agent.akka.app.process

import akka.Done
import akka.actor.typed.ActorSystem
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.ext.ProcessExt.ProcessOps
import esw.agent.akka.app.ext.SpawnCommandExt.SpawnCommandOps
import esw.agent.akka.client.AgentCommand.SpawnCommand
import esw.agent.service.api.models.{Failed, KillResponse, Killed}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal

class ProcessManager(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings
)(implicit system: ActorSystem[_], log: Logger) {
  import system.executionContext

  def spawn(command: SpawnCommand): Future[Either[String, Process]] =
    verifyComponentIsNotAlreadyRegistered(command.connection)
      .flatMapE(_ => startComponent(command))
      .flatMapE(process =>
        waitForRegistration(command.connection, agentSettings.durationToWaitForComponentRegistration).flatMapE(_ =>
          reconcile(process, command.connection)
        )
      )

  // un-registration is done as a part of process.onComplete callback
  def kill(location: Location): Future[KillResponse] =
    getProcessHandle(location) match {
      case Left(e) => Future.successful(Failed(e))
      case Right(process) =>
        process.kill(10.seconds).map(_ => Killed).recover {
          case NonFatal(e) => Failed(s"Failed to kill component process, reason: ${e.getMessage}".tap(log.warn(_)))
        }
    }

  //it creates a process handle with pid extracted from the metadata of the given location
  //and returns the processHandle as the Right value
  //if there is no pid in the location's metadata it returns the error as the Left value
  private def getProcessHandle(location: Location): Either[String, ProcessHandle] =
    location.metadata.getPid.toRight(s"$location metadata does not contain Pid").flatMap(parsePid)

  private def parsePid(pid: Long): Either[String, ProcessHandle] =
    Try(processHandle(pid)).toEither.left
      .map(_.getMessage)
      .flatMap(_.toRight(s"Pid:$pid process does not exist"))

  def processHandle(pid: Long): Option[ProcessHandle] = ProcessHandle.of(pid).toScala

  //It checks if the component of the  given connection if already register in the location service
  //If it is registered then it return an error message string as the Left value in a Future
  //otherwise it return an unit value as Right value in the future
  private def verifyComponentIsNotAlreadyRegistered(connection: Connection): Future[Either[String, Unit]] =
    locationService
      .find(connection.of[Location])
      .map {
        case None    => Right(())
        case Some(l) => Left(s"${connection.componentId} is already registered with location service at $l".tap(log.error(_)))
      }
      .mapError(e => s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_)))

  //starts a process with the executable string of the given spawn command
  private def startComponent(command: SpawnCommand) =
    Future.successful(
      processExecutor
        .runCommand(command.executableCommandStr(agentSettings.coursierChannel, agentSettings.prefix), command.prefix)
        .map(_.tap(onProcessExit(_, command.connection)))
    )

  //it checks if the given process is alive
  //if not it tries to unregister the component of the given connection
  //and returns the error message as a Left value in the Future
  //in case process is still alive it just returns process as the Right value in the Future
  private def reconcile(process: Process, connection: Connection) =
    if (!process.isAlive)
      unregisterComponent(connection).transform(_ =>
        Try(Left("Process terminated before registration was successful".tap(log.warn(_))))
      )
    else Future.successful(Right(process))

  //it checks if the component of the given connection is registered in the location service within the given timeout
  //if not it returns the error message as a Left value in the Future
  //otherwise it just returns unit as the Right value in the Future
  private def waitForRegistration(connection: Connection, timeout: FiniteDuration): Future[Either[String, Unit]] =
    locationService
      .resolve(connection.of[Location], timeout)
      .map {
        case Some(_) => Right(())
        case None    => Left(s"${connection.componentId} is not registered with location service".tap(log.warn(_)))
      }
      .mapError(e => s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_)))

  //it attaches a job to unregister the started component on the completion of the process
  private def onProcessExit(process: Process, connection: Connection): Unit =
    process.toHandle.onComplete { _ =>
      log.warn(s"Process exited with exit value: ${process.exitValue()}, unregistering ${connection.componentId}")
      unregisterComponent(connection)
    }

  private def unregisterComponent(connection: Connection): Future[Done] = locationService.unregister(connection)
}
