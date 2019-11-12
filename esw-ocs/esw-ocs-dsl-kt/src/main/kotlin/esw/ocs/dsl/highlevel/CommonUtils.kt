package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.SequencerCommandServiceFactory
import csw.command.client.messages.ComponentMessage
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.script.utils.SequencerCommandServiceUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import java.util.concurrent.TimeUnit

class CommonUtils(
        private val sequencerAdminFactory: SequencerAdminFactoryApi,
        private val locationServiceUtil: LocationServiceUtil,
        private val lockUnlockUtil: LockUnlockUtil,
        private val actorSystem: ActorSystem<*>,
        private val coroutineScope: CoroutineScope
) {

    private val timeout: Timeout = Timeout(scala.concurrent.duration.Duration.create(10, TimeUnit.SECONDS))
    // fixme: `action` here is a method call on SequencerAdminApi and all API's return Future[T]
    //  so action should be something like => `action: (SequencerAdminApi) -> CompletionStage[T]` and then await on action in the impl
    internal suspend fun sendMsgToSequencer(
            sequencerId: String,
            observingMode: String,
            action: (SequencerAdminApi) -> Unit
    ): Unit = action(sequencerAdminFactory.jMake(sequencerId, observingMode).await())

    suspend fun resolveAssembly(name: String): InternalCommandService {
        val actorRef: ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(name, JComponentType.Assembly()).await()
        val commandServiceUtil = CommandServiceUtil(actorRef, lockUnlockUtil, coroutineScope)
        return InternalCommandService(commandService(name, JComponentType.Assembly()), commandServiceUtil, timeout)
    }

    suspend fun resolveHcd(name: String): InternalCommandService {
        val actorRef: ActorRef<ComponentMessage> = locationServiceUtil.jResolveComponentRef(name, JComponentType.HCD()).await()
        val commandServiceUtil = CommandServiceUtil(actorRef, lockUnlockUtil, coroutineScope)

        return InternalCommandService(commandService(name, JComponentType.HCD()), commandServiceUtil, timeout)
    }

    suspend fun resolveSequencer(sequencerId: String, observingMode: String):InternalSequencerCommandService {
        val sequencerLocation = locationServiceUtil.jResolveSequencer(sequencerId, observingMode, timeout.duration()).await()
        val sequencerCommandService = SequencerCommandServiceFactory.make(sequencerLocation, actorSystem)
        val sequencerAdmin = sequencerAdminFactory.jMake(sequencerId, observingMode).await()

        return InternalSequencerCommandService(SequencerCommandServiceUtil(sequencerCommandService, sequencerAdmin))
    }

    private suspend fun commandService(
            name: String,
            compType: ComponentType
    ): ICommandService = CommandServiceFactory.jMake(locationServiceUtil.jResolveAkkaLocation(name, compType).await(), actorSystem)

}
