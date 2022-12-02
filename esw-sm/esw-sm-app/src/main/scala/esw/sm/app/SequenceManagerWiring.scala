package esw.sm.app

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.config.VersionManager
import esw.commons.utils.location.EswLocationError.RegistrationError
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.handler.SequenceManagerRequestHandler
import esw.sm.impl.config.SequenceManagerConfigParser
import esw.sm.impl.core.SequenceManagerBehavior
import esw.sm.impl.utils.*
import msocket.http.RouteFactory
import msocket.http.post.PostRouteFactory
import msocket.jvm.metrics.LabelExtractor

import java.nio.file.Path
import scala.async.Async.{async, await}
import scala.concurrent.{Await, Future}

class SequenceManagerWiring(
    _port: Option[Int],
    obsModeConfigPath: Path,
    isLocal: Boolean,
    agentPrefix: Option[Prefix],
    simulation: Boolean = false
) {
  private[sm] lazy val smActorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-manager")
  final lazy val actorRuntime = new ActorRuntime(smActorSystem)
  import actorRuntime.*
  private implicit val timeout: Timeout = CommonTimeouts.Wiring
  private val prefix                    = Prefix(ESW, "sequence_manager")

  private lazy val defaultConfig         = smActorSystem.settings.config
  private lazy val versionConfPath: Path = Path.of(defaultConfig.getString("osw.version.confPath"))

  private lazy val locationService: LocationService         = HttpLocationServiceFactory.makeLocalClient(smActorSystem)
  private lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(smActorSystem, locationService)
  private lazy val configUtils: ConfigUtils                 = new ConfigUtils(configClientService)(smActorSystem)
  private lazy val loggerFactory                            = new LoggerFactory(prefix)
  private implicit lazy val logger: Logger                  = loggerFactory.getLogger
  private lazy val versionManager                           = new VersionManager(versionConfPath, configUtils)

  private lazy val locationServiceUtil        = new LocationServiceUtil(locationService)
  private lazy val sequenceComponentAllocator = new SequenceComponentAllocator()
  private lazy val sequenceComponentUtil      = new SequenceComponentUtil(locationServiceUtil, sequenceComponentAllocator)
  private lazy val agentAllocator             = new AgentAllocator()
  private lazy val agentUtil                  = new AgentUtil(locationServiceUtil, agentAllocator, versionManager, simulation)
  private lazy val sequencerUtil              = new SequencerUtil(locationServiceUtil, sequenceComponentUtil)

  private lazy val obsModeConfig =
    Await.result(
      new SequenceManagerConfigParser(configUtils).read(obsModeConfigPath, isLocal),
      CommonTimeouts.Wiring
    )

  private lazy val sequenceManagerBehavior =
    new SequenceManagerBehavior(
      obsModeConfig,
      locationServiceUtil,
      agentUtil,
      sequencerUtil,
      sequenceComponentUtil
    )

  private lazy val sequenceManagerRef: ActorRef[SequenceManagerMsg] = Await.result(
    smActorSystem ? (Spawn(sequenceManagerBehavior.setup, "sequence-manager", Props.empty, _)),
    CommonTimeouts.Wiring
  )

  private lazy val simulationConfigS = s"""
                                          |auth-config {
                                          |  realm = TMT
                                          |  client-id = tmt-backend-app
                                          |  disabled = true
                                          |}""".stripMargin
  private lazy val simulationConfig = ConfigFactory.parseString(simulationConfigS)
  private lazy val config =
    if (simulation) defaultConfig.withValue("auth-config", simulationConfig.getValue("auth-config")) else defaultConfig
  private lazy val connection = AkkaConnection(ComponentId(prefix, ComponentType.Service))
  private lazy val locationMetadata =
    agentPrefix
      .map(Metadata().withAgentPrefix(_))
      .getOrElse(Metadata.empty)
      .withPid(ProcessHandle.current().pid())

  private lazy val registration = AkkaRegistrationFactory.make(connection, sequenceManagerRef, locationMetadata)

  // not marked private, it is overridden in backend-testkit for sequence manager stub wiring
  lazy val sequenceManager: SequenceManagerApi =
    SequenceManagerApiFactory.makeAkkaClient(
      AkkaLocation(registration.connection, registration.actorRefURI, registration.metadata)
    )

  private[esw] lazy val securityDirectives = SecurityDirectives(config, locationService)
  private lazy val postHandler             = new SequenceManagerRequestHandler(sequenceManager, securityDirectives)

  import LabelExtractor.Implicits.default
  import SequenceManagerServiceCodecs.*
  lazy val routes: Route       = RouteFactory.combine(metricsEnabled = false)(new PostRouteFactory("post-endpoint", postHandler))
  private lazy val port: Int   = _port.getOrElse(SocketUtils.getFreePort)
  private lazy val settings    = new Settings(Some(port), Some(prefix), config, ComponentType.Service)
  private lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)
  private lazy val httpServerBinding = httpService.startAndRegisterServer(locationMetadata)

  def start(): Either[RegistrationError, AkkaLocation] = {
    logger.info(s"Starting Sequence Manager with prefix: $prefix")
    if (simulation) logger.info("Starting Sequence Manager in simulation mode")

    Await.result(httpServerBinding, CommonTimeouts.Wiring)

    val loc = Await.result(locationServiceUtil.register(registration), CommonTimeouts.Wiring)

    logger.info(s"Successfully started Sequence Manager with prefix: $prefix")
    loc
  }

  private def shutdownHttpService: Future[Done] =
    async {
      logger.debug("Shutting down Sequence Manager http service")
      val (serverBinding, registrationResult) = await(httpServerBinding)
      val eventualTerminated                  = serverBinding.terminate(CommonTimeouts.Wiring)
      val eventualDone                        = registrationResult.unregister()
      await(eventualTerminated.flatMap(_ => eventualDone))
    }

  def shutdown(reason: CoordinatedShutdown.Reason): Future[Done] =
    shutdownHttpService.flatMap(_ => CoordinatedShutdown(smActorSystem).run(reason))
}

private[sm] object SequenceManagerWiring {
  def apply(
      port: Option[Int],
      obsModeConfig: Path,
      isLocal: Boolean,
      agentPrefix: Option[Prefix],
      _actorSystem: ActorSystem[SpawnProtocol.Command],
      _securityDirectives: SecurityDirectives,
      simulation: Boolean = false
  ): SequenceManagerWiring =
    new SequenceManagerWiring(port, obsModeConfig, isLocal, agentPrefix, simulation) {
      override private[sm] lazy val smActorSystem       = _actorSystem
      override private[esw] lazy val securityDirectives = _securityDirectives
    }
}
