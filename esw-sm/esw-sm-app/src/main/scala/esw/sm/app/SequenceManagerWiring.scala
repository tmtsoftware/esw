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
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import esw.commons.Timeouts
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.impl.core.{SequenceManagerBehavior, SequenceManagerConfigParser}
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

import scala.concurrent.{Await, ExecutionContext, Future}

class SequenceManagerWiring(configPath: Path) {
  private implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequencer-manager")
  private implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  private lazy implicit val timeout: Timeout = Timeouts.DefaultTimeout

  private lazy val locationService: LocationService         = HttpLocationServiceFactory.makeLocalClient(actorSystem)
  private lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val configUtils: ConfigUtils                 = new ConfigUtils(configClientService)(actorSystem)

  private lazy val locationServiceUtil   = new LocationServiceUtil(locationService)
  private lazy val agentUtil             = new AgentUtil(locationServiceUtil)
  private lazy val sequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)
  private lazy val sequencerUtil         = new SequencerUtil(locationServiceUtil, sequenceComponentUtil)

  private lazy val config =
    Await.result(new SequenceManagerConfigParser(configUtils).read(configPath, isLocal = true), Timeouts.DefaultTimeout)

  private lazy val sequenceManagerBehavior = new SequenceManagerBehavior(config, locationServiceUtil, sequencerUtil)(actorSystem)

  private lazy val sequenceManagerRef: ActorRef[SequenceManagerMsg] = Await.result(
    actorSystem ? { replyTo: ActorRef[ActorRef[SequenceManagerMsg]] =>
      Spawn(sequenceManagerBehavior.idle(), "sequence-manager", Props.empty, replyTo)
    },
    Timeouts.DefaultTimeout
  )

  lazy val sequenceManagerApi: SequenceManagerApi = new SequenceManagerImpl(sequenceManagerRef)

  def shutdown(reason: CoordinatedShutdown.Reason): Future[Done] = CoordinatedShutdown(actorSystem).run(reason)
}
