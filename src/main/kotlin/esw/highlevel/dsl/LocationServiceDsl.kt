package esw.highlevel.dsl

import csw.location.models.AkkaLocation
import csw.location.models.ComponentType
import csw.location.models.Location
import csw.params.core.models.Subsystem
import esw.highlevel.dsl.javadsl.JLocationServiceDsl
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import java.util.*

interface LocationServiceDsl : CoroutineScope, JLocationServiceDsl {
    //todo: see if it can be lazy val
    fun strandEc(): StrandEc

    suspend fun listBy(subsystem: Subsystem, componentType: ComponentType): List<AkkaLocation> =
        jListBy(subsystem, componentType, strandEc().ec()).await().toList()

    suspend fun listByComponentName(name: String): List<Location> =
        jListByComponentName(name, strandEc().ec()).await().toList()

    // todo: see if we can use Location? instead of Optional<Location>
    suspend fun resolveByComponentNameAndType(name: String, componentType: ComponentType): Optional<Location> =
        jResolveByComponentNameAndType(name, componentType, strandEc().ec()).await()

    // To be used by Script Writer
    suspend fun resolveSequencer(sequencerId: String, observingMode: String): AkkaLocation =
        jResolveSequencer(sequencerId, observingMode, strandEc().ec()).await()

}