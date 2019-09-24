package esw.dsl.script.services.javadsl

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService
import csw.location.client.extensions.LocationServiceExt.RichLocationService
import csw.location.models._

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

trait JLocationServiceDsl {
  private[esw] val actorSystem: ActorSystem[_]
  private[esw] val _locationService: LocationService
  lazy val locationService: ILocationService = _locationService.asJava

  // it is ok to pass actor system's ec, because only map operations on returned future requires it and does not mutate
  private implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  //TODO: method should filter on all locations instead of AkkaLocations only (b'case Sequencer can have HttpLocation)
  def findSequencer(sequencerId: String, observingMode: String): CompletionStage[AkkaLocation] =
    async {
      await(_locationService.list)
        .find(location => location.connection.componentId.name.contains(s"$sequencerId@$observingMode"))
    }.collect {
      case Some(location: AkkaLocation) => location
      case Some(location) =>
        throw new RuntimeException(s"Sequencer is registered with wrong connection type: ${location.connection.connectionType}")
      case None => throw new IllegalArgumentException(s"Could not find any sequencer with name: $sequencerId@$observingMode")
    }.toJava

}
