package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.IRegistrationResult
import csw.location.models.*
import csw.prefix.models.Prefix
import esw.ocs.dsl.SuspendableConsumer
import esw.ocs.dsl.highlevel.models.RegistrationResult
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.nullable
import kotlinx.coroutines.future.await
import msocket.api.Subscription
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

interface LocationServiceDsl : SuspendToJavaConverter {

    val locationService: ILocationService

    suspend fun register(registration: Registration): RegistrationResult =
            locationService.register(registration).await().toRegistrationResult()

    suspend fun unregister(connection: Connection) {
        locationService.unregister(connection).await()
    }

    suspend fun <L : Location> findLocation(connection: TypedConnection<L>): L? =
            locationService.find(connection).await().nullable()

    suspend fun <L : Location> resolveLocation(connection: TypedConnection<L>, within: Duration = 5.seconds): L? =
            locationService.resolve(connection, within.toJavaDuration()).await().nullable()

    suspend fun listLocations(): List<Location> = locationService.list().await().toList()

    suspend fun listLocationsBy(compType: ComponentType): List<Location> = locationService.list(compType).await().toList()
    suspend fun listLocationsBy(connectionType: ConnectionType): List<Location> = locationService.list(connectionType).await().toList()
    suspend fun listLocationsBy(hostname: String): List<Location> = locationService.list(hostname).await().toList()
    suspend fun listLocationsBy(prefix: Prefix): List<Location> = locationService.listByPrefix(prefix.toString()).await().toList()

    fun onLocationTrackingEvent(connection: Connection, callback: SuspendableConsumer<TrackingEvent>): Subscription =
            locationService.subscribe(connection) { callback.toJava(it) }

    private fun IRegistrationResult.toRegistrationResult() = RegistrationResult(location()) { unregister() }
}