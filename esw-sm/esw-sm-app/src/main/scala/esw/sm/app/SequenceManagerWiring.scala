package esw.sm.app

import java.nio.file.Path

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.Timeouts
import esw.commons.utils.location.EswLocationError.RegistrationError
import esw.commons.utils.location.LocationServiceUtil
import esw.http.core.wiring.ActorRuntime
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.impl.config.SequenceManagerConfigParser
import esw.sm.impl.core.SequenceManagerBehavior
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

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

  private lazy val config =
    Await.result(new SequenceManagerConfigParser(configUtils).read(configPath, isLocal = true), Timeouts.DefaultTimeout)

  private lazy val sequenceManagerBehavior = new SequenceManagerBehavior(config, locationServiceUtil, sequencerUtil)(actorSystem)

  private lazy val sequenceManagerRef: ActorRef[SequenceManagerMsg] = Await.result(
    actorSystem ? (Spawn(sequenceManagerBehavior.idle(), "sequence-manager", Props.empty, _)),
    Timeouts.DefaultTimeout
  )

  def start(): Either[RegistrationError, AkkaLocation] = {
    val registration = AkkaRegistrationFactory.make(AkkaConnection(ComponentId(prefix, Service)), sequenceManagerRef.toURI)
    val loc = Await.result(
      locationServiceUtil.register(registration),
      Timeouts.DefaultTimeout
    )

    logger.info(s"Successfully started Sequence Manager for subsystem: $prefix")
    loc
  }

  def shutdown(reason: CoordinatedShutdown.Reason): Future[Done] = CoordinatedShutdown(actorSystem).run(reason)
}
