package esw.ocs.dsl.highlevel

import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.models.framework.ToComponentLifecycleMessage.`GoOffline$`
import csw.command.client.models.framework.ToComponentLifecycleMessage.`GoOnline$`
import csw.location.models.ComponentType

interface OnlineOfflineDsl {

    val commonUtils: CommonUtils

    suspend fun goOnlineModeForSequencer(sequencerId: String, observingMode: String): Unit {
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) { it.goOnline() }
    }

    suspend fun goOfflineModeForSequencer(sequencerId: String, observingMode: String): Unit {
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) { it.goOffline() }
    }

    suspend fun goOnlineModeForComponent(componentName: String, componentType: ComponentType): Unit =
        commonUtils.sendMsgToComponent(componentName, componentType) {
            it.tell(Lifecycle(`GoOnline$`.`MODULE$`))
        }

    suspend fun goOfflineModeForComponent(componentName: String, componentType: ComponentType): Unit =
        commonUtils.sendMsgToComponent(componentName, componentType) {
            it.tell(Lifecycle(`GoOffline$`.`MODULE$`))
        }
}
