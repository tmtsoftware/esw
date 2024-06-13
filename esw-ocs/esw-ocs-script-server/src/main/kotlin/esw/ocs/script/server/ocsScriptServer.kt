//package esw.ocs.script.server
//
//import org.http4k.core.HttpHandler
//import org.http4k.core.Method.GET
//import org.http4k.core.Method.POST
//import org.http4k.core.Request
//import org.http4k.core.Response
//import org.http4k.core.Status.Companion.OK
//import org.http4k.routing.bind
//import org.http4k.routing.path
//import org.http4k.routing.routes
//import org.http4k.server.Jetty
//import org.http4k.server.asServer
//
//import play.api.libs.json.Json
//import csw.params.core.formats.JavaJsonSupport
//import org.apache.pekko.Done
//import org.apache.pekko.actor.CoordinatedShutdown
//import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
//import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
//import org.apache.pekko.actor.typed.ActorRef
//import org.apache.pekko.actor.typed.ActorSystem
//import org.apache.pekko.actor.typed.Props
//import org.apache.pekko.actor.typed.SpawnProtocol
//import org.apache.pekko.http.scaladsl.server.Route
//import org.apache.pekko.util.Timeout
//import com.typesafe.config.Config
//import csw.aas.http.SecurityDirectives
//import csw.alarm.api.javadsl.IAlarmService
//import csw.alarm.client.AlarmServiceFactory
//import csw.command.client.messages.sequencer.SequencerMsg
//import csw.event.api.scaladsl.EventService
//import csw.event.client.EventServiceFactory
//import csw.event.client.internal.commons.javawrappers.JEventService
//import csw.event.client.models.EventStores.RedisStore
//import csw.location.api.PekkoRegistrationFactory
//import csw.location.api.javadsl.ILocationService
//import csw.location.api.models.*
//import csw.location.api.models.Connection.PekkoConnection
//import csw.location.api.scaladsl.LocationService
//import csw.location.client.ActorSystemFactory
//import csw.location.client.javadsl.JHttpLocationServiceFactory
//import csw.location.client.scaladsl.HttpLocationServiceFactory
//import csw.logging.api.javadsl.ILogger
//import csw.logging.api.scaladsl.Logger
//import csw.logging.client.scaladsl.LoggerFactory
//import csw.network.utils.SocketUtils
//import csw.prefix.models.Prefix
//import csw.prefix.models.Subsystem
//import esw.commons.extensions.FutureEitherExt.FutureEitherJavaOps
//import esw.commons.extensions.FutureEitherExt.FutureEitherOps
//import esw.commons.utils.location.LocationServiceUtil
//import esw.constants.CommonTimeouts
//import esw.http.core.wiring.ActorRuntime
//import esw.http.core.wiring.HttpService
//import esw.http.core.wiring.Settings
//import esw.ocs.api.actor.client.SequencerApiFactory
//import esw.ocs.api.actor.client.SequencerImpl
//import esw.ocs.api.actor.messages.SequencerMessages.Shutdown
//import esw.ocs.api.codecs.SequencerServiceCodecs
//import esw.ocs.api.models.ObsMode
//import esw.ocs.api.models.Variation
//import esw.ocs.api.protocol.ScriptError
//import esw.ocs.api.protocol.ScriptError.LocationServiceError
//import esw.ocs.api.protocol.ScriptError.LoadingScriptFailed
//import esw.ocs.handler.SequencerPostHandler
//import esw.ocs.handler.SequencerWebsocketHandler
//import esw.ocs.impl.blockhound.BlockHoundWiring
//import esw.ocs.impl.core.*
//import esw.ocs.impl.internal.*
//import esw.ocs.impl.script.ScriptApi
//import esw.ocs.impl.script.ScriptContext
//import esw.ocs.impl.script.ScriptLoader
//import io.lettuce.core.RedisClient
//import msocket.http.RouteFactory
//import msocket.http.post.PostRouteFactory
//import msocket.http.ws.WebsocketRouteFactory
//import msocket.jvm.metrics.LabelExtractor
//import cps.compat.FutureAsync.*
//import scala.util.control.NonFatal
//import esw.ocs.app.wiring.SequencerConfig
//import scala.concurrent.Await
//import csw.params.commands.SequenceCommand
//
//
//// Args" <scriptClass> <sequencerPrefix>
//fun main(args: Array<String>) {
//    // XXX TODO parse args
//    val scriptName = args.get(0)
//    val sequencerPrefix: Prefix = Prefix.apply(args.get(1))
//
//    val actorSystem: ActorSystem<SpawnProtocol.Command> = ActorSystemFactory.remote(SpawnProtocol.apply(), "sequencer-system")
//
//    val config: Config = actorSystem.settings().config()
//    val sequencerConfig = SequencerConfig.from(config, sequencerPrefix)
//    val sc = sequencerConfig
//    val timeout: Timeout = Timeout(CommonTimeouts.Wiring())
//    val actorRuntime = ActorRuntime(actorSystem)
//
//    val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorRuntime.typedSystem())
//
//    val sequenceOperatorFactory = null
//    val componentId = ComponentId(sc.prefix(), ComponentType.`Sequencer$`.`MODULE$`)
//
//    val locationServiceUtil = LocationServiceUtil(locationService)(actorRuntime.typedSystem())
//    val jLocationService: ILocationService = JHttpLocationServiceFactory.makeLocalClient(actorSystem)
//
//    val redisClient: RedisClient = RedisClient.create()
//    val eventServiceFactory: EventServiceFactory = EventServiceFactory(RedisStore(redisClient))
//    val eventService: EventService = eventServiceFactory.make(locationService)
//    val jEventService: JEventService = JEventService(eventService)
//    val alarmServiceFactory: AlarmServiceFactory = AlarmServiceFactory(redisClient)
//    val jAlarmService: IAlarmService = alarmServiceFactory.jMakeClientApi(jLocationService, actorSystem)
//
//    val loggerFactory = LoggerFactory(sc.prefix())
//    val logger: Logger = loggerFactory.getLogger()
//    val jLoggerFactory = loggerFactory.asJava()
//    val jLogger: ILogger = ScriptLoader.withScript(sc.scriptClass())(jLoggerFactory.getLogger())
//
//    val sequencerImplFactory = null
//
//    val scriptContext = ScriptContext(
//        sc.heartbeatInterval(),
//        sc.prefix(),
//        ObsMode.from(sc.prefix()),
//        jLogger,
//        sequenceOperatorFactory,
//        actorSystem,
//        jEventService,
//        jAlarmService,
//        sequencerImplFactory,
//        config
//    )
//    val script: ScriptApi = ScriptLoader.loadKotlinScript(sc.scriptClass(), scriptContext)
//    startServer(script)
//}
//
//private fun startServer(script: ScriptApi) {
//    val app: HttpHandler = routes(
//        "/execute" bind POST to {
//            val setup: SequenceCommand = JavaJsonSupport.readSequenceCommand(Json.parse(it.bodyString()))
//            Response(OK).body("pong!")
//        },
////        "/greet/{name}" bind GET to { req: Request ->
////            val name: String? = req.path("name")
////            Response(OK).body("hello ${name ?: "anon!"}")
////        }
//    )
//    app.asServer(Jetty(9000)).start()
//}
