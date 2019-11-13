package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.javadsl.JComponentType
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.highlevel.internal.InternalCommandService
import esw.ocs.dsl.highlevel.internal.InternalSequencerCommandService
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

    fun resolveAssembly(name: String): InternalCommandService {
        val commandServiceUtil = CommandServiceUtil(lockUnlockUtil, coroutineScope)
        return InternalCommandService(name, JComponentType.Assembly(), commandServiceUtil, locationServiceUtil, actorSystem, timeout)
    }

    fun resolveHcd(name: String): InternalCommandService {
        val commandServiceUtil = CommandServiceUtil(lockUnlockUtil, coroutineScope)
        return InternalCommandService(name, JComponentType.HCD(), commandServiceUtil, locationServiceUtil, actorSystem, timeout)
    }

    fun resolveSequencer(sequencerId: String, observingMode: String): InternalSequencerCommandService {
        val sequencerCommandServiceUtil = SequencerCommandServiceUtil(sequencerAdminFactory, locationServiceUtil, actorSystem)
        return InternalSequencerCommandService(sequencerId, observingMode, sequencerCommandServiceUtil)
    }

}
