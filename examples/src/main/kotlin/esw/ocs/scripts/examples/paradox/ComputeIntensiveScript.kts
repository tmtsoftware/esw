package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.scripts.examples.findBigPrime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.math.BigInteger
import kotlin.time.milliseconds

//#call-compute-intensive
script {
  onSetup("prime number") {

    loopAsync(100.milliseconds) {
      // loop represents the computation running on the main script thread.
    }

    // call compute intensive function in async manner
    val bigPrimeNumber: Deferred<BigInteger> = async { findBigPrime() }
    bigPrimeNumber.await()

    // call compute intensive function by suspending
    val bigPrimeNumber2: BigInteger = findBigPrime()

    // --- example ends -------
    // script continues...
  }
}
//#call-compute-intensive

