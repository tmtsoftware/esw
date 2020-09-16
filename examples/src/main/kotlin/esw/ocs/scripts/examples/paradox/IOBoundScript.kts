package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.scripts.examples.findBigPrime
import esw.ocs.scripts.examples.readMessage
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.Reader
import kotlin.time.milliseconds

//#io-bound-call
script {
  onSetup("read file") {

    loopAsync(100.milliseconds) {
      // loop represents the computation running on the main script thread.
    }

    val reader = BufferedReader(FileReader(File("someFile.txt")),10)

    // call io bound function in async manner
    val message1 = async { reader.readMessage() }

    // call io bound function by suspending
    val message2 = reader.readMessage()

    // --- example ends -------
    // script continues...
  }
}
//#io-bound-call

