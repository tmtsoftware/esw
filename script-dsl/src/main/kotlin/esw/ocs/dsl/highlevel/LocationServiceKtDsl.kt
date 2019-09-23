package esw.ocs.dsl.highlevel

import csw.location.models.AkkaLocation
import esw.dsl.script.CswServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await

interface LocationServiceKtDsl : CoroutineScope {
    val cswServices: CswServices

    suspend fun resolveSequencer(sequencerId: String, observingMode: String): AkkaLocation =
        cswServices.findSequencer(sequencerId, observingMode, cswServices.ec()).await()
}
