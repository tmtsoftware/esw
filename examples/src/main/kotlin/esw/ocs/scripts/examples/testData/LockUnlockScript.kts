package esw.ocs.scripts.examples.testData

import csw.command.client.models.framework.LockingResponse
import csw.params.core.models.Prefix
import esw.ocs.dsl.core.script
import kotlin.time.seconds
import kotlin.time.toJavaDuration

script {

    handleSetup("lock-assembly") {
        val response = lockAssembly("test", Prefix("esw.test"), 5.seconds.toJavaDuration())
        assert(response == LockingResponse.lockAcquired())
        publishEvent(SystemEvent("csw.assembly", "lock_response"))
    }


    handleSetup("unlock-assembly") {
        val response = unlockAssembly("test", Prefix("esw.test"))
        assert(response == LockingResponse.lockReleased())
        publishEvent(SystemEvent("csw.assembly", "unlock_response"))
    }
}