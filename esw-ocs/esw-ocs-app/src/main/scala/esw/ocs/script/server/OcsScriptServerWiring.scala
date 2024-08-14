package esw.ocs.script.server

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import org.apache.pekko.util.Timeout
import com.typesafe.config.Config
import cps.compat.FutureAsync.{async, await}
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.javadsl.ILocationService
import csw.location.api.models.*
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.javadsl.ILogger
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.{LoggerFactory, LoggingSystemFactory}
import csw.network.utils.{Networks, SocketUtils}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.impl.core.*
import esw.ocs.impl.script.{ScriptApi, ScriptContext, ScriptLoader}
import io.lettuce.core.RedisClient
import esw.ocs.app.wiring.SequencerConfig
import esw.commons.extensions.FutureEitherExt.{FutureEitherJavaOps, FutureEitherOps}
import esw.ocs.api.SequencerApi
import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import esw.ocs.api.actor.messages.SequencerMessages.Shutdown
import esw.ocs.app.BuildInfo

import scala.concurrent.{Await, Future}

/**
 * Used to start the script server.
 * Note: This contains a lot of code copied from SequencerWiring that could probably be shared,
 * however the intention is to make this a separate project eventually (In Kotlin or Python)
 * SequencerWiring also contains code that we do not need or want here.
 */
//noinspection DuplicatedCode
private[ocs] class OcsScriptServerWiring(sequencerPrefix: Prefix, sequenceComponentPrefix: Prefix) {
  private[ocs] val httpConnection: HttpConnection = HttpConnection(ComponentId(sequencerPrefix, ComponentType.Service))

  private lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  private[ocs] lazy val config: Config  = actorSystem.settings.config
  private[ocs] lazy val sequencerConfig = SequencerConfig.from(config, sequencerPrefix)
  private final lazy val sc             = sequencerConfig
  import sc.*

  implicit lazy val timeout: Timeout = CommonTimeouts.Wiring
  final lazy val actorRuntime        = new ActorRuntime(actorSystem)
  import actorRuntime.{coordinatedShutdown, ec, typedSystem}

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val componentId = ComponentId(prefix, ComponentType.Sequencer)

  // XXX TODO FIXME Await, Option.get
  private def makeSequencerClient(): SequencerApi = {
    try {
      val sequencerLocation =
        Await.result(locationService.resolve(httpConnection, CommonTimeouts.ResolveLocation), CommonTimeouts.ResolveLocation).get
      SequencerApiFactory.make(sequencerLocation)
    }
    catch {
      case ex: Exception =>
        println(s"Failed to locate Sequencer Client for httpConnection: $httpConnection")
        ex.printStackTrace()
        throw ex
    }
  }
  private val sequencerClient = makeSequencerClient()

  // Pass lambda to break circular dependency shown below.
  // SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperatorHttp(sequencerClient)
  private[ocs] lazy val script: ScriptApi  = ScriptLoader.loadKotlinScript(scriptClass, scriptContext)

  private lazy val locationServiceUtil                = new LocationServiceUtil(locationService)
  private lazy val jLocationService: ILocationService = JHttpLocationServiceFactory.makeLocalClient(actorSystem)

  private lazy val redisClient: RedisClient                 = RedisClient.create()
  private lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  private lazy val eventService: EventService               = eventServiceFactory.make(locationService)
  private lazy val jEventService: JEventService             = new JEventService(eventService)
  private lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  private lazy val jAlarmService: IAlarmService             = alarmServiceFactory.jMakeClientApi(jLocationService, actorSystem)

  // Disable logging for tests
  if (!sys.props.get("test.esw").contains("true"))
    actorRuntime.startLogging("SequencerApp", BuildInfo.version)

  private lazy val loggerFactory    = new LoggerFactory(prefix)
  private lazy val logger: Logger   = loggerFactory.getLogger
  private lazy val jLoggerFactory   = loggerFactory.asJava
  private lazy val jLogger: ILogger = ScriptLoader.withScript(scriptClass)(jLoggerFactory.getLogger)

  private lazy val sequencerImplFactory =
    (_subsystem: Subsystem, _obsMode: ObsMode, _variation: Option[Variation]) => // todo: revisit timeout value
      locationServiceUtil
        .resolveSequencerHttp(Variation.prefix(_subsystem, _obsMode, _variation), CommonTimeouts.ResolveLocation)
        .mapRight(SequencerApiFactory.make)
        .toJava

  private[ocs] val shutdownHttpService: () => Future[Done] = () =>
    async {
      logger.debug("Shutting down Sequencer http service")
      val (serverBinding, registrationResult) = await(httpServerBinding)
      val eventualTerminated                  = serverBinding.terminate(CommonTimeouts.Wiring)
      val eventualDone                        = registrationResult.unregister()
      val f                                   = await(eventualTerminated.flatMap(_ => eventualDone))
      actorSystem.terminate()
      f
    }

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, sequenceComponentPrefix, logger, shutdownHttpService)(
      actorSystem
    )

  private lazy val scriptContext = new ScriptContext(
    heartbeatInterval,
    prefix,
    ObsMode.from(prefix),
    jLogger,
    sequenceOperatorFactory,
    actorSystem,
    jEventService,
    jAlarmService,
    sequencerImplFactory,
    config
  )

  private lazy val routes      = OcsScriptServerRoutes(logger, script, this).route
  private lazy val metadata    = Metadata().withSequenceComponentPrefix(sequenceComponentPrefix)
  private lazy val settings    = new Settings(Some(SocketUtils.getFreePort), Some(prefix), config, ComponentType.Service)
  private lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime, NetworkType.Inside)
  private lazy val httpServerBinding = httpService.startAndRegisterServer(metadata)
  Await.result(httpServerBinding, CommonTimeouts.Wiring)

  private def cleanupResources() = {
    redisClient.shutdown()
//    (sequencerRef ? Shutdown.apply).map(_ => Done)
    Future.successful(Done)
  }

  coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseBeforeServiceUnbind,
    s"${prefix.toString()}-cleanup"
  )(() => cleanupResources())

}
