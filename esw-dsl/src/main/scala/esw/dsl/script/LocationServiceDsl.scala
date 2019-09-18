package esw.dsl.script

import csw.location.api.scaladsl.LocationService
import csw.location.models.AkkaLocation

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

trait LocationServiceDsl {

  private[esw] val locationService: LocationService

  def resolveSequencer(sequencerId: String, observingMode: String)(implicit ec: ExecutionContext): Future[AkkaLocation] =
    async {
      await(locationService.list)
        .find(location => location.connection.componentId.name.contains(s"$sequencerId@$observingMode"))
    }.collect {
      case Some(location: AkkaLocation) => location
      case Some(location) =>
        throw new RuntimeException(s"Sequencer is registered with wrong connection type: ${location.connection.connectionType}")
      case None => throw new IllegalArgumentException(s"Could not find any sequencer with name: $sequencerId@$observingMode")
    }

}
