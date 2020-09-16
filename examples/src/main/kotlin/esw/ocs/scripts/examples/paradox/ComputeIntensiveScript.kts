package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.scripts.examples.findBigPrime
import kotlinx.coroutines.async
import kotlin.time.milliseconds

//#call-compute-intensive
script {
  onSetup("prime number") {

    loopAsync(100.milliseconds) {
      // loop represents the computation running on the main script thread.
    }

    // call compute intensive function in async manner
    val bigPrimeNumber = async { findBigPrime() }
    bigPrimeNumber.await()

    // call compute intensive function by suspending
    val bigPrimeNumber2 = findBigPrime()

    // --- example ends -------
    // script continues...
  }
}
//#call-compute-intensive

