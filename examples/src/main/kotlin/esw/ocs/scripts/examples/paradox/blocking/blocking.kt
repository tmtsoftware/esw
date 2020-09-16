package esw.ocs.scripts.examples.paradox.blocking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.math.BigInteger
import java.util.*

//#compute-intensive-function
// Calculating probablePrime is cpu bound operation and should be wrapped inside Default dispatcher
// Following function takes around 10 seconds to find a 4096 bit length prime number
suspend fun findBigPrime(): BigInteger =
        withContext(Dispatchers.Default) {
            BigInteger.probablePrime(4096, Random())
        }
//#compute-intensive-function

//#io-bound-function
// Reading a line from a file is blocking IO operation and should be wrapped inside IO dispatcher
suspend fun BufferedReader.readMessage(): CharSequence? =
        withContext(Dispatchers.IO) {
            readLine()
        }
//#io-bound-function
