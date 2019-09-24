package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.ILocationService
import csw.location.models.AkkaLocation
import csw.location.models.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await

interface LocationServiceKtDsl : CoroutineScope {
    val locationService: ILocationService

    suspend fun resolveSequencer(sequencerId: String, observingMode: String): AkkaLocation {
        locationService.list().await()
            .find { location -> location.connection().componentId().name().contains("$sequencerId@$observingMode") }
            .let {
                when (it) {
                    is AkkaLocation -> return it
                    is Location -> throw RuntimeException("Sequencer is registered with wrong connection type: ${it.connection().connectionType()}")
                    else -> throw IllegalArgumentException("Could not find any sequencer with name: $sequencerId@$observingMode")
                }
            }
    }
}
