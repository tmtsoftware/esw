package esw.ocs.scripts.examples.testData

import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse.*
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.stringKey
import kotlin.time.milliseconds

script {
    val lockResponseEvent = SystemEvent("esw.ocs.lock_unlock", "locking_response")
    val key = stringKey("lockingResponse")
    val assembly = Assembly("test")

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