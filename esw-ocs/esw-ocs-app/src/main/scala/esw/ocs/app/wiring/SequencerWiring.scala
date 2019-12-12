package esw.ocs.app.wiring

import akka.Done
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.api.javadsl.IAlarmService
import csw.command.client.messages.sequencer.SequencerMsg
import csw.config.api.javadsl.IConfigClientService
import csw.config.client.javadsl.JConfigClientFactory
import csw.database.DatabaseServiceFactory
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.javadsl.ILocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.logging.api.javadsl.ILogger
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.SocketUtils
import csw.params.core.models.Subsystem
import esw.http.core.wiring.{ActorRuntime, CswWiring, HttpService, Settings}
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.ScriptError
import esw.ocs.dsl.script.utils.{LockUnlockUtil, ScriptLoader}
import esw.ocs.dsl.script.{CswServices, ScriptDsl}
import esw.ocs.handler.{SequencerPostHandler, SequencerWebsocketHandler}
import esw.ocs.impl.core._
import esw.ocs.impl.internal.{LocationServiceUtil, SequencerServer, Timeouts}
import esw.ocs.impl.messages.SequencerMessages.Shutdown
import esw.ocs.impl.syntax.FutureSyntax.FutureOps
import esw.ocs.impl.{SequencerActorProxy, SequencerActorProxyFactory}
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory
import msocket.impl.{Encoding, RouteFactory}

import scala.async.Async.{async, await}
import scala.util.control.NonFatal

private[ocs] class SequencerWiring(
    val subsystem: Subsystem,
    val observingMode: String,
    sequenceComponentLocation: AkkaLocation
) extends SequencerHttpCodecs {
  private lazy val config: Config  = ConfigFactory.load()
  private lazy val sequencerConfig = SequencerConfig.from(config, subsystem, observingMode)
  import sequencerConfig._

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  implicit lazy val timeout: Timeout = Timeouts.DefaultTimeout
  lazy val cswWiring: CswWiring      = CswWiring.make(actorSystem)
  import cswWiring._
  import cswWiring.actorRuntime._

  implicit lazy val actorRuntime: ActorRuntime = cswWiring.actorRuntime

  lazy val sequencerRef: ActorRef[SequencerMsg] = (typedSystem ? { x: ActorRef[ActorRef[SequencerMsg]] =>
    Spawn(sequencerBehavior.setup, prefix.value, Props.empty, x)
  }).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperatorImpl(sequencerRef)
  private lazy val componentId             = ComponentId(prefix, ComponentType.Sequencer)
  private lazy val script: ScriptDsl       = ScriptLoader.loadKotlinScript(scriptClass, cswServices)

  private lazy val locationServiceUtil   = new LocationServiceUtil(locationService)
  private lazy val sequencerProxyFactory = new SequencerActorProxyFactory(locationServiceUtil)

  lazy private val lockUnlockUtil = new LockUnlockUtil(prefix)(actorSystem)

  lazy val jLocationService: ILocationService         = JHttpLocationServiceFactory.makeLocalClient(actorSystem)
  lazy val jConfigClientService: IConfigClientService = JConfigClientFactory.clientApi(actorSystem, jLocationService)
  lazy val jEventService: JEventService               = new JEventService(eventService)

  private lazy val jAlarmService: IAlarmService = alarmServiceFactory.jMakeClientApi(jLocationService, typedSystem)

  private lazy val loggerFactory    = new LoggerFactory(prefix.value)
  private lazy val jLoggerFactory   = loggerFactory.asJava
  private lazy val logger: Logger   = loggerFactory.getLogger
  private lazy val jLogger: ILogger = ScriptLoader.withScript(scriptClass)(jLoggerFactory.getLogger)

  private lazy val databaseServiceFactory = new DatabaseServiceFactory(actorSystem)

  lazy val cswServices = new CswServices(
    sequenceOperatorFactory,
    jLogger,
    typedSystem,
    jLocationService,
    jEventService,
    timeServiceSchedulerFactory,
    sequencerProxyFactory,
    databaseServiceFactory,
    lockUnlockUtil,
    jConfigClientService,
    jAlarmService
  )

  private lazy val sequencerApi                              = new SequencerActorProxy(sequencerRef)
  private lazy val postHandler                               = new SequencerPostHandler(sequencerApi)
  private def websocketHandlerFactory(encoding: Encoding[_]) = new SequencerWebsocketHandler(sequencerApi, encoding)

  lazy val routes: Route = RouteFactory.combine(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory)
  )

  private lazy val settings    = new Settings(Some(SocketUtils.getFreePort), Some(prefix), config, ComponentType.Sequencer)
  private lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)

  private val shutdownHttpService = () =>
    async {
      val (serverBinding, registrationResult) = await(httpService.registeredLazyBinding)
      val eventualTerminated                  = serverBinding.terminate(Timeouts.DefaultTimeout)
      val eventualDone                        = registrationResult.unregister()
      await(eventualTerminated.flatMap(_ => eventualDone))
    }

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, sequenceComponentLocation, shutdownHttpService)(typedSystem)

  lazy val sequencerServer: SequencerServer = new SequencerServer {
    override def start(): Either[ScriptError, AkkaLocation] = {
      try {
        new Engine(script).start(sequenceOperatorFactory())

        httpService.registeredLazyBinding.block

        val registration = AkkaRegistration(AkkaConnection(componentId), sequencerRef.toURI)
        locationServiceUtil.register(registration).block
      }
      catch {
        case NonFatal(e) => Left(ScriptError(e.getMessage))
      }
    }

    override def shutDown(): Done = (sequencerRef ? Shutdown).map(_ => Done).block
  }

}
