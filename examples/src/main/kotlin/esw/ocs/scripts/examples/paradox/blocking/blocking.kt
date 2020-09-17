package esw.ocs.scripts.examples.paradox.blocking

import esw.ocs.dsl.blockingCpu
import esw.ocs.dsl.blockingIo
import java.io.BufferedReader
import java.math.BigInteger
import java.util.*

//#compute-intensive-function
// Calculating probablePrime is cpu bound operation and should be wrapped inside blockingCpu utility function
// Following function takes around 10 seconds to find a 4096 bit length prime number
suspend fun findBigPrime(): BigInteger =
        blockingCpu {
            BigInteger.probablePrime(4096, Random())
        }
//#compute-intensive-function

//#io-bound-function
// Reading a line from a file is blocking IO operation and should be wrapped inside blockingIo utility function
suspend fun BufferedReader.readMessage(): CharSequence? =
        blockingIo {
            readLine()
        }
//#io-bound-function
