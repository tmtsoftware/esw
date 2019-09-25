package esw.sm.app

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.http.core.wiring.ActorRuntime
import esw.sm.api.SequenceManagerMsg
import esw.sm.impl.SequenceManagerBehaviour

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class SequenceManagerWiring {

  private lazy val _actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "SM-system")
  private lazy implicit val timeout: Timeout                = Timeout(10.seconds)

  private lazy val actorRuntime = new ActorRuntime(_actorSystem)
  import actorRuntime._

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private lazy val locationServiceUtil              = new LocationServiceUtil(locationService)

  lazy val sequenceManagerRef: ActorRef[SequenceManagerMsg] =
    Await.result(typedSystem ? Spawn(SequenceManagerBehaviour.behaviour(locationServiceUtil), "sequencer-manager"), 5.seconds)
}
