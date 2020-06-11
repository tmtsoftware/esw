package esw.sm.app

import java.net.URI
import java.nio.file.Path

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.Config
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.Timeouts
import esw.commons.utils.location.EswLocationError.RegistrationError
import esw.commons.utils.location.LocationServiceUtil
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.handler.{SequenceManagerPostHandler, SequenceManagerWebsocketHandler}
import esw.sm.impl.config.SequenceManagerConfigParser
import esw.sm.impl.core.SequenceManagerBehavior
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}
import msocket.api.ContentType
import msocket.impl.RouteFactory
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory

import scala.async.Async.{async, await}
import scala.concurrent.{Await, Future}

class SequenceManagerWiring(configPath: Path) {
  private lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-manager")
  lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._
  private implicit val timeout: Timeout = Timeouts.DefaultTimeout

  private val prefix = Prefix(ESW, "sequence_manager")

  private lazy val locationService: LocationService         = HttpLocationServiceFactory.makeLocalClient(actorSystem)
  private lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val configUtils: ConfigUtils                 = new ConfigUtils(configClientService)(actorSystem)
  private lazy val loggerFactory                            = new LoggerFactory(prefix)
  private lazy val logger: Logger                           = loggerFactory.getLogger

  private lazy val locationServiceUtil   = new LocationServiceUtil(locationService)
  private lazy val agentUtil             = new AgentUtil(locationServiceUtil)
  private lazy val sequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)
  private lazy val sequencerUtil         = new SequencerUtil(locationServiceUtil, sequenceComponentUtil)

  private lazy val smConfig =
    Await.result(new SequenceManagerConfigParser(configUtils).read(configPath, isLocal = true), Timeouts.DefaultTimeout)

  private lazy val sequenceManagerBehavior = new SequenceManagerBehavior(smConfig, locationServiceUtil, sequencerUtil)(
    actorSystem
  )

  private lazy val sequenceManagerRef: ActorRef[SequenceManagerMsg] = Await.result(
    actorSystem ? (Spawn(sequenceManagerBehavior.idle(), "sequence-manager", Props.empty, _)),
    Timeouts.DefaultTimeout
  )

  private lazy val config: Config                      = actorSystem.settings.config
  private lazy val connection                          = AkkaConnection(ComponentId(prefix, ComponentType.Service))
  private lazy val refURI: URI                         = sequenceManagerRef.toURI
  private lazy val sequenceManager: SequenceManagerApi = new SequenceManagerImpl(AkkaLocation(connection, refURI))

  private lazy val postHandler                                 = new SequenceManagerPostHandler(sequenceManager)
  private def websocketHandlerFactory(contentTye: ContentType) = new SequenceManagerWebsocketHandler(sequenceManager, contentTye)

  import SequenceManagerHttpCodec._
  lazy val routes: Route = RouteFactory.combine(metricsEnabled = false)(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory)
  )

  private lazy val settings    = new Settings(Some(SocketUtils.getFreePort), Some(prefix), config, ComponentType.Service)
  private lazy val httpService = new HttpService(logger, locationService, routes, settings, actorRuntime)

  def start(): Either[RegistrationError, AkkaLocation] = {
    logger.info(s"Starting Sequence Manager with prefix: $prefix")
    //start http server and register it with location service
    Await.result(httpService.registeredLazyBinding, Timeouts.DefaultTimeout)

    val registration = AkkaRegistrationFactory.make(connection, refURI)
    val loc = Await.result(
      locationServiceUtil.register(registration),
      Timeouts.DefaultTimeout
    )

    logger.info(s"Successfully started Sequence Manager with prefix: $prefix")
    loc
  }

  private def shutdownHttpService: Future[Done] =
    async {
      logger.debug("Shutting down Sequencer Manager http service")
      val (serverBinding, registrationResult) = await(httpService.registeredLazyBinding)
      val eventualTerminated                  = serverBinding.terminate(Timeouts.DefaultTimeout)
      val eventualDone                        = registrationResult.unregister()
      await(eventualTerminated.flatMap(_ => eventualDone))
    }

  def shutdown(reason: CoordinatedShutdown.Reason): Future[Done] =
    shutdownHttpService.flatMap(_ => CoordinatedShutdown(actorSystem).run(reason))
}
