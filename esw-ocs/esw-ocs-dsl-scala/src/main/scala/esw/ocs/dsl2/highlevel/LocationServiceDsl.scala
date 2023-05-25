package esw.ocs.dsl2.highlevel

import akka.Done
import csw.location.api.models.{ComponentType, Connection, ConnectionType, Location, Registration, TrackingEvent, TypedConnection}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import msocket.api.Subscription

import async.Async.*
import scala.concurrent.ExecutionContext

class LocationServiceDsl(locationService: LocationService)(using ExecutionContext) {

  inline def register(registration: Registration): RegistrationResult =
    await(locationService.register(registration))

  inline def unregister(connection: Connection): Done =
    await(locationService.unregister(connection))

  inline def findLocation[L <: Location](connection: TypedConnection[L]): Option[L] =
    await(locationService.find(connection))

  inline def listLocations(): List[Location] = await(locationService.list)

  inline def listLocationsBy(compType: ComponentType): List[Location]        = await(locationService.list(compType))
  inline def listLocationsBy(connectionType: ConnectionType): List[Location] = await(locationService.list(connectionType))
  inline def listLocationsByHostname(hostname: String): List[Location]       = await(locationService.list(hostname))
  inline def listLocationsBy(prefixStartsWith: String): List[Location] = await(locationService.listByPrefix(prefixStartsWith))

  inline def onLocationTrackingEvent(connection: Connection, inline callback: TrackingEvent => Unit): Subscription =
    locationService.subscribe(connection, e => async(callback(e)))

}
