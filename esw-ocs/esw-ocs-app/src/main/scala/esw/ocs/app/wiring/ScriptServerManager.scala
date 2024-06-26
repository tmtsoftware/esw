package esw.ocs.app.wiring

import com.typesafe.config.Config
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.models.{ComponentId, ComponentType, Connection, HttpLocation, Location}
import csw.location.api.models.Connection.{HttpConnection, PekkoConnection}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import esw.agent.pekko.app.process.{ProcessExecutor, ProcessOutput}
import esw.agent.service.api.models.{Failed, KillResponse, Killed}
import esw.commons.utils.config.{FetchingScriptVersionFailed, VersionManager}
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.prefix.models.Prefix
import esw.agent.pekko.app.process.cs.CoursierLaunch
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.agent.pekko.app.ext.ProcessExt.ProcessOps
import esw.ocs.app.client.OcsScriptClient
import esw.ocs.impl.script.ScriptApi
import esw.ocs.script.server.OcsScriptServerApp
import org.apache.pekko.Done

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.jdk.OptionConverters.RichOptional
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * Start sequencer script HTTP server.
 * This code is based on the Agent service code, but copied here, assuming the script server will be
 * started by the esw-ocs-app (SequencerApp) process and not the agent process.
 * Also, if the Kotlin script implementation is replaced with a Python implementation, then Coursier would
 * no longer be used to start the server.
 *
 * @param prefix used to register and lookup the script server with the location service
 * @param locationService CSW location service
 * @param config application/actor system config
 * @param log logger
 */
class ScriptServerManager(prefix: Prefix, locationService: LocationService, config: Config, log: Logger)(implicit
    typedSystem: ActorSystem[SpawnProtocol.Command],
    ec: ExecutionContext
) {
  private val configClientService                    = ConfigClientFactory.clientApi(typedSystem, locationService)
  private val configUtils                            = new ConfigUtils(configClientService)
  private val versionConfPath: Path                  = Path.of(config.getString("osw.version.confPath"))
  private val versionManager                         = new VersionManager(versionConfPath, configUtils)
  private val processOutput                          = new ProcessOutput()
  private val processExecutor                        = new ProcessExecutor(processOutput)(log)
  private val connection: HttpConnection             = HttpConnection(ComponentId(prefix, ComponentType.Service))
  private val coursierChannel                        = config.getString("agent.coursier.channel")
  private val durationToWaitForComponentRegistration = 18.seconds

  /**
   * Starts the script HTTP server and returns an HTTP client for it that implements the ScriptApi trait
   */
  def spawn(): Future[Either[String, ScriptApi]] = {
    println("XXX Start script server in new process")
    verifyComponentIsNotAlreadyRegistered(connection)
      .flatMapE(_ => startScriptServer())
      .flatMapE(process =>
        waitForRegistration(connection, durationToWaitForComponentRegistration)
          .flatMapE { loc =>
            reconcile(process, loc, connection)
          }
          .flatMapE { loc =>
            Future.successful(Right(OcsScriptClient(loc)))
          }
      )
  }

  /**
   * Starts the script HTTP server in this process (for testing) and returns an HTTP client for it that implements the ScriptApi trait
   */
  def start(): Future[Either[String, ScriptApi]] = {
    println("XXX Start script server for testing")
    verifyComponentIsNotAlreadyRegistered(connection)
      .flatMapE(_ =>
        OcsScriptServerApp.main(Array(prefix.toString))
        waitForRegistration(connection, durationToWaitForComponentRegistration)
          .flatMapE { loc =>
            Future.successful(Right(OcsScriptClient(loc)))
          }
      )
  }

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
  private def verifyComponentIsNotAlreadyRegistered(connection: Connection): Future[Either[String, Unit]] = {
    println(s"XXX Verify script server not running at $connection")
    locationService
      .find(connection.of[Location])
      .map {
        case None =>
          println("XXX Script server was not running")
          Right(())
        case Some(l) => Left(s"${connection.componentId} is already registered with location service at $l".tap(log.error(_)))
      }
      .mapError(e => s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_)))
  }

  // starts a process with the executable string of the given spawn command
  private def startScriptServer(): Future[Either[String, Process]] = {

    //    versionManager.getScriptVersion
//      .map { version =>
//        val cmdStr = CoursierLaunch("esw-ocs-script-server-app", Some(version)).launch(coursierChannel, List(prefix.toString))
//        processExecutor
//          .runCommand(cmdStr, prefix)
//          .map(_.tap(onProcessExit(_, connection)))
//      }
//      .recover { case FetchingScriptVersionFailed(msg) =>
//        Left(msg)
//      }

    Future.successful {
      val cmdStr =
        CoursierLaunch("esw-ocs-script-server-app", Some("0.1.0-SNAPSHOT")).launch(coursierChannel, List(prefix.toString))
      println(s"XXX ScriptServerManager.startScriptServer: cmd = $cmdStr")
      processExecutor
        .runCommand(cmdStr, prefix)
        .map(_.tap(onProcessExit(_, connection)))
    }
  }

  // it checks if the given process is alive
  // if not it tries to unregister the component of the given connection
  // and returns the error message as a Future
  // in case process is still alive it returns the location as a Future
  private def reconcile(process: Process, loc: HttpLocation, connection: Connection): Future[Either[String, HttpLocation]] = {
    if (!process.isAlive)
      unregisterComponent(connection).transform(_ =>
        Try(Left("Process terminated before registration was successful".tap(log.warn(_))))
      )
    else Future.successful(Right(loc))
  }

  // it checks if the component of the given connection is registered in the location service within the given timeout
  // if not it returns the error message as a Future
  // otherwise it returns the location
  private def waitForRegistration(connection: Connection, timeout: FiniteDuration): Future[Either[String, HttpLocation]] = {
    println(s"XXX waiting for reg of ${connection.of[Location]} with timeout $timeout")
    locationService
      .resolve(connection.of[Location], timeout)
      .map {
        case Some(loc) =>
          println(s"XXX resolved location at $loc")
          Right(loc.asInstanceOf[HttpLocation])
        case None =>
          println(s"XXX could not resolve location of $connection")
          Left(
            s"${connection.componentId} is not registered with location service. Reason: Process failed to spawn due to reasons like invalid binary version etc or failed to register with location service."
              .tap(log.warn(_))
          )
      }
      .mapError { e =>
        println(s"XXX waitForRegistration failed: $e")
        s"Failed to verify component registration in location service, reason: ${e.getMessage}".tap(log.error(_))
      }
  }

  // it attaches a job to unregister the started component on the completion of the process
  private def onProcessExit(process: Process, connection: Connection): Unit = {
    process.toHandle.onComplete { _ =>
      log.warn(s"Process exited with exit value: ${process.exitValue()}, unregistering ${connection.componentId}")
      unregisterComponent(connection)
    }
  }

  private def unregisterComponent(connection: Connection): Future[Done] = locationService.unregister(connection)
}
