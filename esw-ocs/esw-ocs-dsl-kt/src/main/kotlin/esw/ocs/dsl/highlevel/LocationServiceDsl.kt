package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.IRegistrationResult
import csw.location.api.models.*
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

    /**
     * Registers registration details
     *
     * @param registration holds a connection, and it's corresponding location information
     * @return RegistrationResult which contains registered location and handle for un-registration
     */
    suspend fun register(registration: Registration): RegistrationResult =
            locationService.register(registration).await().toRegistrationResult()

    /**
     * Unregisters the connection
     *
     * @param connection an already registered connection
     */
    suspend fun unregister(connection: Connection) {
        locationService.unregister(connection).await()
    }

    /**
     * Look up for the location registered against provided connection
     *
     * @param connection a connection to be looked up
     * @return location registered against connection if present otherwise null
     */
    suspend fun <L : Location> findLocation(connection: TypedConnection<L>): L? =
            locationService.find(connection).await().nullable()

    /**
     * Resolve the location registered against provided connection
     *
     * @param connection a connection to be resolved
     * @param within optional duration for which connection is looked up in the Location Service before giving up
     * @return location registered against connection if present otherwise null
     */
    suspend fun <L : Location> resolveLocation(connection: TypedConnection<L>, within: Duration = 5.seconds): L? =
            locationService.resolve(connection, within.toJavaDuration()).await().nullable()

    /**
     * Lists all the registered locations
     */
    suspend fun listLocations(): List<Location> = locationService.list().await().toList()

    /**
     * Lists all the registered locations matching against provided component type
     */
    suspend fun listLocationsBy(compType: ComponentType): List<Location> = locationService.list(compType).await().toList()

    /**
     * Lists all the registered locations matching against provided connection type
     */
    suspend fun listLocationsBy(connectionType: ConnectionType): List<Location> = locationService.list(connectionType).await().toList()

    /**
     * Lists all the registered locations matching against provided hostname
     */
    suspend fun listLocationsBy(hostname: String): List<Location> = locationService.list(hostname).await().toList()

    /**
     * Lists all the registered locations matching against provided prefix
     */
    suspend fun listLocationsBy(prefix: Prefix): List<Location> = locationService.listByPrefix(prefix.toString()).await().toList()

    /**
     * Subscribe to the connection and executes a callback on every location changed event
     *
     * @param connection a connection to be tracked
     * @param callback task which consumes [TrackingEvent] and returns [Unit]
     * @return an [Subscription] object which has a handle for canceling subscription
     */
    fun onLocationTrackingEvent(connection: Connection, callback: SuspendableConsumer<TrackingEvent>): Subscription =
            locationService.subscribe(connection) { callback.toJava(it) }

    private fun IRegistrationResult.toRegistrationResult() = RegistrationResult(location()) { unregister() }
}