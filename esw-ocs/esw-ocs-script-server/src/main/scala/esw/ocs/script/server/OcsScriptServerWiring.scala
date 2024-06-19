package esw.ocs.script.server

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.util.Timeout
import com.typesafe.config.Config
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.javadsl.ILocationService
import csw.location.api.models.*
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.javadsl.ILogger
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts
import esw.http.core.wiring.ActorRuntime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.impl.core.*
import esw.ocs.impl.script.{ScriptApi, ScriptContext, ScriptLoader}
import io.lettuce.core.RedisClient
import esw.ocs.app.wiring.SequencerConfig

import java.util.concurrent.CompletionStage

class OcsScriptServerWiring(sequencerPrefix: Prefix) {
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  private[ocs] lazy val config: Config  = actorSystem.settings.config
  private[ocs] lazy val sequencerConfig = SequencerConfig.from(config, sequencerPrefix)
  final lazy val sc                     = sequencerConfig
  import sc.*

  implicit lazy val timeout: Timeout = CommonTimeouts.Wiring
  final lazy val actorRuntime        = new ActorRuntime(actorSystem)
  import actorRuntime.{ec, typedSystem}

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  // Not needed here
  private lazy val sequenceOperatorFactory: () => SequenceOperator = null
  private lazy val componentId                                     = ComponentId(prefix, ComponentType.Sequencer)
  private[ocs] lazy val script: ScriptApi                          = ScriptLoader.loadKotlinScript(scriptClass, scriptContext)

  private lazy val locationServiceUtil        = new LocationServiceUtil(locationService)
  lazy val jLocationService: ILocationService = JHttpLocationServiceFactory.makeLocalClient(actorSystem)

  lazy val redisClient: RedisClient                 = RedisClient.create()
  lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  lazy val eventService: EventService               = eventServiceFactory.make(locationService)
  lazy val jEventService: JEventService             = new JEventService(eventService)
  lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  private lazy val jAlarmService: IAlarmService     = alarmServiceFactory.jMakeClientApi(jLocationService, actorSystem)

  private lazy val loggerFactory    = new LoggerFactory(prefix)
  private lazy val logger: Logger   = loggerFactory.getLogger
  private lazy val jLoggerFactory   = loggerFactory.asJava
  private lazy val jLogger: ILogger = ScriptLoader.withScript(scriptClass)(jLoggerFactory.getLogger)

  // not needed here
  private lazy val sequencerImplFactory: (Subsystem, ObsMode, Option[Variation]) => CompletionStage[SequencerApi] = null

  lazy val scriptContext = new ScriptContext(
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

  val port                    = 9000 // XXX FIXME or 0?
  val server: OcsScriptServer = OcsScriptServer(port, OcsScriptServerRoutes(logger, script).route)
}
