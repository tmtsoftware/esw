/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.app.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.Config
import csw.aas.http.SecurityDirectives
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.javadsl.ILocationService
import csw.location.api.models.*
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.javadsl.ILogger
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.SocketUtils
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.{FutureEitherJavaOps, FutureEitherOps}
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import esw.ocs.api.actor.client.{SequencerApiFactory, SequencerImpl}
import esw.ocs.api.actor.messages.SequencerMessages.Shutdown
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.ScriptError.{LoadingScriptFailed, LocationServiceError}
import esw.ocs.handler.{SequencerPostHandler, SequencerWebsocketHandler}
import esw.ocs.impl.blockhound.BlockHoundWiring
import esw.ocs.impl.core.*
import esw.ocs.impl.internal.*
import esw.ocs.impl.script.{ScriptApi, ScriptContext, ScriptLoader}
import io.lettuce.core.RedisClient
import msocket.http.RouteFactory
import msocket.http.post.PostRouteFactory
import msocket.http.ws.WebsocketRouteFactory
import msocket.jvm.metrics.LabelExtractor

import scala.async.Async.{async, await}
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

// $COVERAGE-OFF$
private[ocs] class SequencerWiring(val sequencerPrefix: Prefix, sequenceComponentPrefix: Prefix) extends SequencerServiceCodecs {
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sequencer-system")

  private[ocs] lazy val config: Config  = actorSystem.settings.config
  private[ocs] lazy val sequencerConfig = SequencerConfig.from(config, sequencerPrefix)
  import sequencerConfig.*

  implicit lazy val timeout: Timeout = CommonTimeouts.Wiring
  lazy val actorRuntime              = new ActorRuntime(actorSystem)
  import actorRuntime.{coordinatedShutdown, ec, typedSystem}

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  lazy val sequencerRef: ActorRef[SequencerMsg] = Await.result(
    actorSystem ? { x: ActorRef[ActorRef[SequencerMsg]] =>
      Spawn(sequencerBehavior.setup, prefix.toString, Props.empty, x)
    },
    CommonTimeouts.Wiring
  )

  // Pass lambda to break circular dependency shown below.
  // SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperator(sequencerRef)
  private lazy val componentId             = ComponentId(prefix, ComponentType.Sequencer)
  private[ocs] lazy val script: ScriptApi  = ScriptLoader.loadKotlinScript(scriptClass, scriptContext)

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

  private lazy val sequencerImplFactory =
    (_subsystem: Subsystem, _obsMode: ObsMode, _variation: Option[Variation]) => // todo: revisit timeout value
      locationServiceUtil
        .resolveSequencer(Variation.prefix(_subsystem, _obsMode, _variation), CommonTimeouts.ResolveLocation)
        .mapRight(SequencerApiFactory.make)
        .toJava

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

  private lazy val sequencerApi            = new SequencerImpl(sequencerRef)
  private lazy val securityDirectives      = SecurityDirectives.authDisabled(config)
  private lazy val postHandler             = new SequencerPostHandler(sequencerApi, securityDirectives)
  private lazy val websocketHandlerFactory = new SequencerWebsocketHandler(sequencerApi)

  import LabelExtractor.Implicits.default
  lazy val routes: Route = RouteFactory.combine(metricsEnabled = false)(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory)
  )

  private lazy val metadata    = Metadata().withSequenceComponentPrefix(sequenceComponentPrefix)
  private lazy val settings    = new Settings(Some(SocketUtils.getFreePort), Some(prefix), config, ComponentType.Sequencer)
  private lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime, NetworkType.Inside)
  private lazy val httpServerBinding = httpService.startAndRegisterServer(metadata)

  private val shutdownHttpService: () => Future[Done] = () =>
    async {
      logger.debug("Shutting down Sequencer http service")
      val (serverBinding, registrationResult) = await(httpServerBinding)
      val eventualTerminated                  = serverBinding.terminate(CommonTimeouts.Wiring)
      val eventualDone                        = registrationResult.unregister()
      await(eventualTerminated.flatMap(_ => eventualDone))
    }

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, sequenceComponentPrefix, logger, shutdownHttpService)(
      actorSystem
    )

  lazy val sequencerServer: SequencerServer = new SequencerServer {
    override def start(): Either[ScriptError, AkkaLocation] = {
      try {
        logger.info(
          s"Starting sequencer for subsystem: ${sequencerPrefix.subsystem} with observing mode: ${sequencerPrefix.componentName}"
        )
        new Engine(script).start(sequenceOperatorFactory())

        Await.result(httpServerBinding, CommonTimeouts.Wiring)

        val registration = AkkaRegistrationFactory.make(AkkaConnection(componentId), sequencerRef, metadata)
        val loc = Await.result(
          locationServiceUtil.register(registration).mapLeft(e => LocationServiceError(e.msg)),
          CommonTimeouts.Wiring
        )

        coordinatedShutdown.addTask(
          CoordinatedShutdown.PhaseBeforeServiceUnbind,
          s"${prefix.toString()}-cleanup"
        )(() => cleanupResources())

        logger.info(
          s"Successfully started Sequencer for subsystem: ${sequencerPrefix.subsystem} with observing mode: ${sequencerPrefix.componentName}"
        )
        if (enableThreadMonitoring) {
          logger.info(s"Thread Monitoring enabled for ${BlockHoundWiring.integrations}")
          BlockHoundWiring.install()
        }
        loc
      }
      catch {
        // This error will be logged in SequenceComponent.Do not log it here,
        // because exception caused while initialising will fail the instance creation of logger.
        case NonFatal(e) =>
          terminateActorSystem()
          Left(LoadingScriptFailed(e.getMessage))
      }
    }

    override def shutDown(): Done = {
      Await.result(CoordinatedShutdown(actorSystem).run(CoordinatedShutdown.actorSystemTerminateReason), CommonTimeouts.Wiring)
    }

    private def cleanupResources() = {
      redisClient.shutdown()
      (sequencerRef ? Shutdown).map(_ => Done)
    }
  }

  private def terminateActorSystem(): Done = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, CommonTimeouts.Wiring)
  }
}
// $COVERAGE-ON$
