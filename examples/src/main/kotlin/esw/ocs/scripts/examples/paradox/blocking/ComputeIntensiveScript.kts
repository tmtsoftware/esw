@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox.blocking

import esw.ocs.dsl.core.script
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.math.BigInteger
import kotlin.time.Duration

//#call-compute-intensive
script {

    loopAsync(Duration.milliseconds(100)) {
        // loop represents the computation running on the main script thread.
    }

    onSetup("prime number") {
        // by default calling findBigPrime cpu intensive task suspends and waits for result
        // but this runs on different thread than the main script thread
        // which allows other background tasks started previously to run concurrently
        val bigPrime1: BigInteger = findBigPrime()

        // if you want to run findBigPrime in the background, then wrap it within async
        val bigPrimeDeferred: Deferred<BigInteger> = async { findBigPrime() }
        // ...
        // wait for compute intensive operation to finish which was previously started
        val bigPrime2: BigInteger = bigPrimeDeferred.await()

        // script continues...
    }
}
//#call-compute-intensive

