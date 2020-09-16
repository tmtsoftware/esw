package esw.ocs.scripts.examples

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.*

//#compute-intensive-function
// Calculating probablePrime is cpu bound operation and
// following function takes around 10 seconds to find a 4096 bit length prime number.
suspend fun findBigPrime(): BigInteger =
        withContext(Dispatchers.Default) {
            BigInteger.probablePrime(4096, Random())
        }
//#compute-intensive-function
