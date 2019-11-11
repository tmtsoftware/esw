package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.location.models.ComponentType
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.future.await
import scala.compat.java8.FutureConverters
import scala.concurrent.Future

class CommonUtils(private val sequencerAdminFactory: SequencerAdminFactoryApi, private val locationServiceUtil: LocationServiceUtil) {

    internal suspend fun <T> sendMsgToSequencer(
        sequencerId: String,
        observingMode: String,
        action: suspend (SequencerAdminApi) -> Future<T>
    ): T {
        val resultF = action(sequencerAdminFactory.jMake(sequencerId, observingMode).await())
        return FutureConverters.toJava(resultF).await()
    }

    internal suspend fun sendMsgToComponent(
        componentName: String,
        componentType: ComponentType,
        callback: (ActorRef<ComponentMessage>) -> Unit
    ): Unit = callback(locationServiceUtil.jResolveComponentRef(componentName, componentType).await())
}
