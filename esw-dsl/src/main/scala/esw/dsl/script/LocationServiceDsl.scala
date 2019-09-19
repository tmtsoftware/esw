package esw.dsl.script

import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType.Sequencer
import csw.location.models.{AkkaLocation, ComponentId}
import csw.location.models.Connection.AkkaConnection
import esw.dsl.Timeouts

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

// fixme: implicit ec in script is strand ec, do we need that here?
//  can we use actor systems ec here and remove implicit ec from each api?
trait LocationServiceDsl {

  private[esw] val locationService: LocationService

  //todo: merge findSequencer and resolveSequencer
  def findSequencer(sequencerId: String, observingMode: String)(implicit ec: ExecutionContext): Future[AkkaLocation] =
    async {
      await(locationService.list)
      //fixme: sequencer has two registrations - http and akka, contains check will return any but we need akka here
        .find(location => location.connection.componentId.name.contains(s"$sequencerId@$observingMode"))
    }.collect {
      case Some(location: AkkaLocation) => location
      case Some(location) =>
        throw new RuntimeException(s"Sequencer is registered with wrong connection type: ${location.connection.connectionType}")
      case None => throw new IllegalArgumentException(s"Could not find any sequencer with name: $sequencerId@$observingMode")
    }

  def resolveSequencer(sequencerId: String, observingMode: String)(implicit ec: ExecutionContext): Future[AkkaLocation] =
    locationService
      .resolve(AkkaConnection(ComponentId(s"$sequencerId@$observingMode", Sequencer)), Timeouts.DefaultTimeout)
      .collect {
        case Some(location: AkkaLocation) => location
        case Some(location) =>
          throw new RuntimeException(s"Sequencer is registered with wrong connection type: ${location.connection.connectionType}")
        case None => throw new IllegalArgumentException(s"Could not find any sequencer with name: $sequencerId@$observingMode")
      }

}
