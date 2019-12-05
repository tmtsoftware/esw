package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.intKey
import kotlinx.coroutines.launch

script {
    var counter = 0

    // keep incrementing counter 10_0000 times in the background while processing commands
    val job = coroutineScope.launch {
        repeat(10_0000) { counter++ }
    }

    onSetup("increment") {
        repeat(10_0000) { counter++ }
        counter++
    }

    onObserve("get-counter") {
        job.join()
        publishEvent(ObserveEvent("esw.counter", "get-counter", intKey("counter").set(counter)))
    }
}