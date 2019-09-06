package esw.ocs.app

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import caseapp.{CommandApp, RemainingArgs}
import csw.framework.internal.wiring.ActorRuntime
import csw.location.client.utils.LocationServerStatus
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.network.utils.SocketUtils
import esw.http.core.wiring.{HttpService, ServerWiring}
import esw.ocs.api.models.responses.RegistrationError
import esw.ocs.app.SequencerAppCommand._
import esw.ocs.impl.SequencerAdminImpl
import esw.ocs.internal.{SequenceComponentWiring, SequencerWiring, Timeouts}

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def sequencerWiringWithHttp(
      sequencerId: String,
      observingMode: String,
      sequenceComponentName: Option[String]
  ): SequencerWiring =
    new SequencerWiring(sequencerId, observingMode, sequenceComponentName) {
      import sequencerConfig.sequencerName

      private lazy val serverWiring = new ServerWiring(Some(SocketUtils.getFreePort), Some(s"$sequencerName@http"))
      import serverWiring._
      import cswCtx._

      private implicit val timeout: Timeout                   = Timeouts.DefaultTimeout
      private implicit val system: ActorSystem[SpawnProtocol] = actorSystem
      private lazy val sequencerAdmin                         = new SequencerAdminImpl(sequencerRef)
      private lazy val postHandler                            = new PostHandlerImpl(sequencerAdmin)
      private lazy val routes                                 = new SequencerAdminRoutes(postHandler)

      private lazy val httpService =
        new HttpService(logger, locationService, routes.route, settings, serverWiring.actorRuntime)

      override def shutDown(): Future[Done] = {
        httpService.shutdown(UnknownReason)
        super.shutDown()
      }

      override def start(): Either[RegistrationError, AkkaLocation] = {
        Await.result(httpService.registeredLazyBinding, timeout.duration)
        super.start()
      }
    }

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): Unit =
    command match {
      case SequenceComponent(prefix) =>
        val wiring = new SequenceComponentWiring(prefix, sequencerWiringWithHttp)
        startSequenceComponent(wiring, enableLogging)

      case Sequencer(id, mode) =>
        val wiring: SequencerWiring = sequencerWiringWithHttp(id, mode, None)
        startSequencer(wiring, enableLogging)
    }

  def startSequenceComponent(sequenceComponentWiring: SequenceComponentWiring, enableLogging: Boolean): Unit = {
    import sequenceComponentWiring._
    withLogging(actorRuntime, cswServicesWiring.log, enableLogging) {
      sequenceComponentWiring.start()
    }
  }

  def startSequencer(sequencerWiring: SequencerWiring, enableLogging: Boolean): Unit = {
    import sequencerWiring._
    withLogging(actorRuntime, cswServicesWiring.log, enableLogging) {
      sequencerWiring.start()
    }
  }

  private def withLogging(actorRuntime: ActorRuntime, log: Logger, enableLogging: Boolean)(
      f: => Either[RegistrationError, AkkaLocation]
  ): Unit = {
    import actorRuntime._
    def cleanup(): Unit = typedSystem.terminate()
    try {
      if (enableLogging) startLogging(typedSystem.name)
      report(f, log, enableLogging)(() => cleanup())
    } catch {
      case NonFatal(e) => cleanup(); throw e
    }
  }

  private def report(either: Either[RegistrationError, AkkaLocation], log: Logger, enableLogging: Boolean)(
      cleanup: () => Unit
  ): Unit =
    either match {
      case Left(err) =>
        cleanup()
        val errMsg = s"Failed to start with error: $err"
        log.error(errMsg)
        printLogs("ERROR", errMsg, enableLogging)
        exit(255)
      case Right(location) =>
        val msg = s"Successfully started and registered Component with Location: [$location]"
        log.info(msg)
        printLogs("INFO", msg, enableLogging)
    }

  private def printLogs(level: String, msg: String, enableLogging: Boolean): Unit = if (enableLogging) {
    println(s"[$level] $msg")
    println(s"[$level] Please find complete logs under ${sys.env("TMT_LOG_HOME")} directory")
  }
}
