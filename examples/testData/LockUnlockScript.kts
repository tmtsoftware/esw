package esw.ocs.scripts.examples.testData

import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse.lockExpired
import csw.command.client.models.framework.LockingResponse.lockExpiringShortly
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.params.stringKey
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

script {
    val lockResponseEvent = SystemEvent("ESW.ocs.lock_unlock", "locking_response")
    val key = stringKey("lockingResponse")
    val assembly = Assembly(ESW, "test", 10.seconds)

    suspend fun publishLockingResponse(lockingResponse: LockingResponse) {
        publishEvent(lockResponseEvent.add(key.set(lockingResponse.javaClass.simpleName)))
    }

    onSetup("lock-assembly") {
        val initialLockResponse = assembly.lock(
                leaseDuration = 200.milliseconds,
                onLockAboutToExpire = { publishLockingResponse(lockExpiringShortly()) },
                onLockExpired = { publishLockingResponse(lockExpired()) }
        )

        publishLockingResponse(initialLockResponse)
    }

    onSetup("unlock-assembly") {
        val response = assembly.unlock()
        publishLockingResponse(response)
    }
}