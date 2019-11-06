package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.location.models.ComponentType
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.future.await

class CommonUtils(private val sequencerAdminFactory: SequencerAdminFactoryApi, private val locationServiceUtil: LocationServiceUtil) {

    // fixme: `action` here is a method call on SequencerAdminApi and all API's return Future[T]
    //  so action should be something like => `action: (SequencerAdminApi) -> CompletionStage[T]` and then await on action in the impl
    internal suspend fun sendMsgToSequencer(
        sequencerId: String,
        observingMode: String,
        action: (SequencerAdminApi) -> Unit
    ): Unit = action(sequencerAdminFactory.jMake(sequencerId, observingMode).await())

    internal suspend fun sendMsgToComponent(
        componentName: String,
        componentType: ComponentType,
        callback: (ActorRef<ComponentMessage>) -> Unit
    ): Unit = callback(locationServiceUtil.jResolveComponentRef(componentName, componentType).await())
}
