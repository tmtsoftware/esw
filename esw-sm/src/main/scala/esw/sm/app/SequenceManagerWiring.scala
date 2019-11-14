package esw.sm.app

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import esw.http.core.wiring.ActorRuntime
import esw.ocs.api.SequencerCommandFactoryApi
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.impl.SequencerCommandFactoryImpl
import esw.sm.api.SequenceManagerMsg
import esw.sm.impl.SequenceManagerBehaviour

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class SequenceManagerWiring {

  private lazy val _actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SM-system")
  private lazy implicit val timeout: Timeout                        = Timeout(10.seconds)

  private lazy val actorRuntime = new ActorRuntime(_actorSystem)
  import actorRuntime._

  private lazy val locationService: LocationService                    = HttpLocationServiceFactory.makeLocalClient
  private lazy val locationServiceUtil                                 = new LocationServiceUtil(locationService)
  private lazy val sequencerCommandFactory: SequencerCommandFactoryApi = new SequencerCommandFactoryImpl(locationServiceUtil)

  lazy val sequenceManagerRef: ActorRef[SequenceManagerMsg] =
    Await.result(
      typedSystem ? { x =>
        Spawn(
          SequenceManagerBehaviour.behaviour(locationServiceUtil, sequencerCommandFactory),
          "sequencer-manager",
          Props.empty,
          x
        )
      },
      5.seconds
    )
}
