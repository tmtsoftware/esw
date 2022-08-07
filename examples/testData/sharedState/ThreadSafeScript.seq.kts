package esw.ocs.scripts.examples.testData.sharedState

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.intKey
import esw.ocs.scripts.examples.SharedConstants.events
import kotlinx.coroutines.launch

script {
    // keep incrementing counter 100_000 times in the background while processing commands
    val job = coroutineScope.launch {
        repeat(100_000) { counter++ }
    }

    loadScripts(counterIncrementer)

    onObserve("get-counter") {
        job.join()
        publishEvent(events.getCounter.add(intKey("counter").set(counter)))
    }
}