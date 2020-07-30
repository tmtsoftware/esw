package esw.agent.app.process

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.client.scaladsl.GenericLoggerFactory
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.api.AgentCommand.SpawnCommand.{SpawnManuallyRegistered, SpawnSelfRegistered}
import esw.agent.api.AgentCommand.{ProcessExited, SpawnCommand}
import esw.agent.api._
import esw.agent.app.AgentSettings
import esw.agent.app.ext.FutureEitherExt.FutureEitherOps
import esw.agent.app.ext.ProcessExt.ProcessOps
import esw.agent.app.process.cs.Coursier
import esw.agent.app.process.redis.Redis

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal

class ProcessManager(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings
)(implicit system: ActorSystem[_]) {
  import agentSettings._
  import system.executionContext

  private val log = GenericLoggerFactory.getLogger

  def spawn(command: SpawnCommand, agentActor: ActorRef[AgentCommand]): Future[Either[String, Process]] =
    verifyComponentIsNotAlreadyRegistered(command.connection)
      .flatMapE(_ => startComponent(command, agentActor))
      .flatMapE(process => waitForComponentRegistration(command).flatMapE(_ => reconcile(process, command.connection)))

  // un-registration is done as a part of process.onComplete callback
  def kill(process: Process): Future[KillResponse] =
    process.kill(10.seconds).map(_ => Killed).recover {
      case NonFatal(e) => Failed(s"Failed to kill component process, reason: $e".warn)
    }

  private def executableCommand(command: SpawnCommand): List[String] =
    command match {
      case SpawnSequenceComponent(_, _, version)  => Coursier.ocsApp(version).launch(coursierChannel, command.commandArgs)
      case SpawnSequenceManager(_, _, _, version) => Coursier.smApp(version).launch(coursierChannel, command.commandArgs)
      case _: SpawnRedis                          => Redis.server :: command.commandArgs
    }

  private def verifyComponentIsNotAlreadyRegistered(connection: Connection): Future[Either[String, Unit]] =
    checkRegistration(connection, 0.seconds) {
      case None => Right(())
      case Some(l) =>
        Left(s"Component ${connection.componentId.fullName} is already registered with location service at location $l".warn)
    }

  private def startComponent(command: SpawnCommand, agentActor: ActorRef[AgentCommand]) =
    Future.successful(
      processExecutor.runCommand(executableCommand(command), command.prefix).map { p =>
        onProcessExit(p, command.connection, agentActor); p
      }
    )

  private def reconcile(process: Process, connection: Connection) =
    if (!process.isAlive)
      unregisterComponent(connection).transform(_ => Try(Left("Process terminated before registration was successful".warn)))
    else Future.successful(Right(process))

  private def waitForComponentRegistration(command: SpawnCommand): Future[Either[String, Unit]] =
    command match {
      case _: SpawnSelfRegistered       => waitForRegistration(command.connection, durationToWaitForComponentRegistration)
      case cmd: SpawnManuallyRegistered => registerComponent(cmd.registration)
    }

  private def addUnRegistrationHook(result: RegistrationResult): Unit =
    CoordinatedShutdown(system)
      .addTask(PhaseBeforeServiceUnbind, s"unregister-${result.location.connection.componentId.fullName}") { () =>
        result.unregister()
      }

  private def registerComponent(registration: Registration): Future[Either[String, Unit]] =
    locationService.register(registration).map { r => Right(addUnRegistrationHook(r)) }.recover {
      case NonFatal(e) =>
        val compName = registration.connection.componentId.fullName
        Left(s"Failed to register component $compName with location service, reason: ${e.getMessage}".err)
    }

  private def unregisterComponent(connection: Connection): Future[Done] = locationService.unregister(connection)

  private def checkRegistration(connection: Connection, timeout: FiniteDuration)(
      check: Option[Location] => Either[String, Unit]
  ): Future[Either[String, Unit]] =
    locationService.resolve(connection.of[Location], timeout).map(check).recover {
      case NonFatal(e) => Left(s"Failed to verify component registration in location service, reason: ${e.getMessage}".err)
    }

  private def waitForRegistration(connection: Connection, timeout: FiniteDuration): Future[Either[String, Unit]] =
    checkRegistration(connection, timeout) {
      case Some(_) => Right(())
      case None    => Left(s"Component ${connection.componentId.fullName} is not registered with location service".warn)
    }

  private def onProcessExit(process: Process, connection: Connection, agentActor: ActorRef[AgentCommand]): Unit =
    process.onComplete { _ =>
      val compId = connection.componentId
      log.warn(s"Process exited with exit value: ${process.exitValue()}, unregistering component ${compId.fullName}")
      agentActor ! ProcessExited(compId)
      unregisterComponent(connection)
    }

  private implicit class StringLogOps(private val msg: String) {
    def warn: String = msg.tap(log.warn(_))
    def err: String  = msg.tap(log.error(_))
  }
}
