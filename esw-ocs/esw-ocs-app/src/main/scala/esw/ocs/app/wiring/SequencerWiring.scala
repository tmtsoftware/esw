package esw.ocs.app.wiring

import akka.Done
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.api.javadsl.IAlarmService
import csw.command.client.messages.sequencer.SequencerMsg
import csw.config.api.javadsl.IConfigClientService
import csw.config.client.javadsl.JConfigClientFactory
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.javadsl.ILocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.SocketUtils
import esw.http.core.wiring.{ActorRuntime, CswWiring, HttpService, Settings}
import esw.ocs.api.protocol.LoadScriptError
import esw.ocs.app.route.{PostHandlerImpl, SequencerAdminRoutes, WebsocketHandlerImpl}
import esw.ocs.dsl.script.utils.{LockUnlockUtil, ScriptLoader}
import esw.ocs.dsl.script.{CswServices, JScriptDsl}
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.impl.core._
import esw.ocs.impl.internal.{SequencerServer, Timeouts}
import esw.ocs.impl.messages.SequencerMessages.Shutdown
import esw.ocs.impl.syntax.FutureSyntax.FutureOps
import esw.ocs.impl.{SequencerAdminFactoryImpl, SequencerAdminImpl}
import msocket.impl.Encoding

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.util.control.NonFatal

private[ocs] class SequencerWiring(val packageId: String, val observingMode: String, sequenceComponentName: Option[String]) {
  private lazy val config: Config       = ConfigFactory.load()
  private[esw] lazy val sequencerConfig = SequencerConfig.from(config, packageId, observingMode, sequenceComponentName)
  import sequencerConfig._

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  implicit lazy val timeout: Timeout = Timeouts.DefaultTimeout
  lazy val cswWiring: CswWiring      = CswWiring.make(actorSystem)
  import cswWiring._
  import cswWiring.actorRuntime._

  lazy val loggerFactory  = new LoggerFactory(sequencerName)
  lazy val logger: Logger = loggerFactory.getLogger

  implicit lazy val actorRuntime: ActorRuntime = cswWiring.actorRuntime

  lazy val sequencerRef: ActorRef[SequencerMsg] = (typedSystem ? { x: ActorRef[ActorRef[SequencerMsg]] =>
    Spawn(sequencerBehavior.setup, sequencerName, Props.empty, x)
  }).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperatorImpl(sequencerRef)
  private lazy val componentId             = ComponentId(sequencerName, ComponentType.Sequencer)
  private lazy val script: JScriptDsl      = ScriptLoader.loadKotlinScript(scriptClass, cswServices)

  lazy private val locationServiceUtil = new LocationServiceUtil(locationService)
  lazy private val adminFactory        = new SequencerAdminFactoryImpl(locationServiceUtil)

  lazy private val lockUnlockUtil = new LockUnlockUtil(locationServiceUtil)(actorSystem)

  lazy val jLocationService: ILocationService         = JHttpLocationServiceFactory.makeLocalClient(actorSystem, actorRuntime.mat)
  lazy val jConfigClientService: IConfigClientService = JConfigClientFactory.clientApi(actorSystem, jLocationService)
  lazy val jEventService: JEventService               = new JEventService(eventService)

  private lazy val jAlarmService: IAlarmService = alarmServiceFactory.jMakeClientApi(jLocationService, typedSystem)

  lazy val cswServices = new CswServices(
    sequenceOperatorFactory,
    typedSystem,
    jLocationService,
    jEventService,
    timeServiceSchedulerFactory,
    adminFactory,
    lockUnlockUtil,
    jConfigClientService,
    jAlarmService
  )

  private lazy val sequencerAdmin                            = new SequencerAdminImpl(sequencerRef)
  private lazy val postHandler                               = new PostHandlerImpl(sequencerAdmin)
  private def websocketHandlerFactory(encoding: Encoding[_]) = new WebsocketHandlerImpl(sequencerAdmin, encoding)
  private lazy val routes                                    = new SequencerAdminRoutes(postHandler, websocketHandlerFactory)

  private lazy val settings = new Settings(Some(SocketUtils.getFreePort), Some(s"$sequencerName@http"), config)
  private lazy val httpService: HttpService =
    new HttpService(logger, locationService, routes.route, settings, actorRuntime)

  private val shutdownHttpService = () =>
    async {
      val (serverBinding, registrationResult) = await(httpService.registeredLazyBinding)
      val eventualTerminated                  = serverBinding.terminate(Timeouts.DefaultTimeout)
      val eventualDone                        = registrationResult.unregister()
      await(eventualTerminated.flatMap(_ => eventualDone))
    }

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, shutdownHttpService)(
      typedSystem
    )

  lazy val sequencerServer: SequencerServer = new SequencerServer {
    override def start(): Either[LoadScriptError, AkkaLocation] = {
      try {
        new Engine(script).start(sequenceOperatorFactory())

        httpService.registeredLazyBinding.block

        val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)
        new LocationServiceUtil(locationService).register(registration).block
      } catch {
        case NonFatal(e) => Left(LoadScriptError(e.getMessage))
      }
    }

    override def shutDown(): Future[Done] = (sequencerRef ? Shutdown).map(_ => Done)
  }

}
