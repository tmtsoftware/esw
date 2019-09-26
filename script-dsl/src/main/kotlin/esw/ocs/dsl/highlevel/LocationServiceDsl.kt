package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.ILocationService
import csw.location.models.AkkaLocation
import csw.location.models.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await

interface LocationServiceDsl : CoroutineScope {
    val locationService: ILocationService

    suspend fun resolveSequencer(sequencerId: String, observingMode: String): AkkaLocation {
        fun getSequencerLocation(locations: List<Location>): AkkaLocation {
            if (locations.isEmpty()) throw IllegalArgumentException("Could not find any sequencer with name: $sequencerId@$observingMode")
            val first = locations.component1()

            return when {
                first is AkkaLocation -> first
                locations.drop(1).isEmpty() -> throw RuntimeException("Sequencer is registered with wrong connection type: ${first.connection().connectionType()}")
                else -> getSequencerLocation(locations.drop(1))
            }
        }

        val sequencerLocations: List<Location> = locationService.list().await()
            .filter { it.connection().componentId().name().contains("$sequencerId@$observingMode") }

        return getSequencerLocation(sequencerLocations)
    }
}
