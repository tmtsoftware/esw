@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox.blocking

import esw.ocs.dsl.core.script
import kotlinx.coroutines.async
import java.io.File
import kotlin.time.Duration

//#io-bound-call
script {
    loopAsync(Duration.milliseconds(100)) {
        // loop represents the computation running on the main script thread.
    }

    onSetup("read file") {
        val reader = File("someFile.txt").bufferedReader()

        // by default calling readMessage (blocking io) task suspends and waits for result
        // but this runs on different thread than the main script thread
        // which allows other background tasks started previously to run concurrently
        val message1 = readMessage(reader)

        // if you want to run readMessage in the background, then wrap it within async
        val message2Deferred = async { readMessage(reader) }
        // ...
        // wait for blocking operation to finish which was previously started
        val message2: CharSequence? = message2Deferred.await()

        // script continues...
    }
}
//#io-bound-call

