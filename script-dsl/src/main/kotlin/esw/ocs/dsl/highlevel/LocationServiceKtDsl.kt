package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.ILocationService
import csw.location.models.AkkaLocation
import csw.location.models.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await

interface LocationServiceKtDsl : CoroutineScope {
    val locationService: ILocationService

    suspend fun resolveSequencer(sequencerId: String, observingMode: String): AkkaLocation {
        val location: Location? = locationService.list().await()
            .find { it.connection().componentId().name().contains("$sequencerId@$observingMode") }

        when (location) {
            is AkkaLocation -> return location
            is Location -> throw RuntimeException("Sequencer is registered with wrong connection type: ${location.connection().connectionType()}")
            else -> throw IllegalArgumentException("Could not find any sequencer with name: $sequencerId@$observingMode")
        }
    }
}
