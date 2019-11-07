package esw.ocs.scripts.examples.testData

import csw.command.client.models.framework.LockingResponse.*
import csw.params.core.models.Prefix
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.stringKey
import kotlinx.coroutines.delay
import kotlin.time.milliseconds

script {

    handleSetup("lock-assembly") {
        val lockResponseEvent = SystemEvent("esw.test", "lock_response")
        val key = stringKey("lockResponse")
        val lockAcquiredEvent = lockResponseEvent.add(key.set("LockAcquired"))
        val lockExpiringShortlyEvent = lockResponseEvent.add(key.set("LockExpiringShortly"))
        val lockExpiredEvent = lockResponseEvent.add(key.set("LockExpired"))

        lockAssembly("test", Prefix("esw.test"), 200.milliseconds) { lockResponse ->
            when (lockResponse) {
                lockAcquired() -> publishEvent(lockAcquiredEvent)
                lockExpiringShortly() -> publishEvent(lockExpiringShortlyEvent)
                `LockExpired$`.`MODULE$` -> publishEvent(lockExpiredEvent)
                else -> throw RuntimeException("Unknown LockResponse: $it received")
            }
        }
    }

    handleSetup("unlock-assembly") {
        val key = stringKey("unlockResponse")
        val unlockResponseEvent = SystemEvent("esw.test", "unlock_response")

        val response = unlockAssembly("test", Prefix("esw.test"))
        assert(response == lockAlreadyReleased())
        publishEvent(unlockResponseEvent.add(key.set("LockAlreadyReleased")))
    }
}