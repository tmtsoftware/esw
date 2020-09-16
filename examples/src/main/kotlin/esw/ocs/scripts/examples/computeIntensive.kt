package esw.ocs.scripts.examples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.*

//#compute-intensive-function
// The following function takes around 10 seconds to find a 4096 bit length prime number.
suspend fun findBigPrime(): BigInteger {
  return withContext(Dispatchers.Default) {
      BigInteger.probablePrime(4096, Random())
  }
}
//#compute-intensive-function
