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
  import agentSettings._
  import system.executionContext

  def spawn(command: SpawnCommand): Future[Either[String, Process]] =
    verifyComponentIsNotAlreadyRegistered(command.connection)
      .flatMapE(_ => startComponent(command))
      .flatMapE(process =>
        waitForRegistration(command.connection, durationToWaitForComponentRegistration).flatMapE(_ =>
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

  def getProcessHandle(location: Location): Either[String, ProcessHandle] =
    location.metadata.getPID
      .toRight(s"$location metadata does not contain Pid")
      .flatMap(parsePid)

  def parsePid(pid: String): Either[String, ProcessHandle] =
    Try(processHandle(pid)).toEither.left
      .map(_.getMessage)
      .flatMap(_.toRight(s"Pid:$pid process does not exist"))

  def processHandle(pid: String): Option[ProcessHandle] = ProcessHandle.of(pid.toLong).toScala

  private def verifyComponentIsNotAlreadyRegistered(connection: Connection): Future[Either[String, Unit]] =
    checkRegistration(connection, 0.seconds) {
      case None => Right(())
      case Some(l) =>
        val name = connection.componentId.fullName
        Left(s"Component $name is already registered with location service at location $l".tap(log.warn(_)))
    }

  private def startComponent(command: SpawnCommand) =
    Future.successful(
      processExecutor
        .runCommand(command.executableCommandStr(coursierChannel, agentSettings.prefix), command.prefix)
        .map(_.tap(onProcessExit(_, command.connection)))
    )

  private def reconcile(process: Process, connection: Connection) =
    if (!process.isAlive)
      unregisterComponent(connection).transform(_ =>
        Try(Left("Process terminated before registration was successful".tap(log.warn(_))))
      )
    else Future.successful(Right(process))

  private def unregisterComponent(connection: Connection): Future[Done] = locationService.unregister(connection)

  private def checkRegistration(connection: Connection, timeout: FiniteDuration)(
      check: Option[Location] => Either[String, Unit]
  ): Future[Either[String, Unit]] =
    locationService.resolve(connection.of[Location], timeout).map(check).recover {
      case NonFatal(e) =>
        Left(s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_)))
    }

  private def waitForRegistration(connection: Connection, timeout: FiniteDuration): Future[Either[String, Unit]] =
    checkRegistration(connection, timeout) {
      case Some(_) => Right(())
      case None    => Left(s"Component ${connection.componentId.fullName} is not registered with location service".tap(log.warn(_)))
    }

  private def onProcessExit(process: Process, connection: Connection): Unit =
    process.toHandle.onComplete { _ =>
      val compId = connection.componentId
      log.warn(s"Process exited with exit value: ${process.exitValue()}, unregistering component ${compId.fullName}")
      unregisterComponent(connection)
    }
}
