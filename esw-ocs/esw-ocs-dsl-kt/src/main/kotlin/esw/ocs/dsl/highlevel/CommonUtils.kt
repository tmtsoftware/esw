package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.javadsl.JComponentType
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.dsl.Timeouts
import esw.ocs.dsl.highlevel.RichCommandService
import esw.ocs.dsl.highlevel.RichSequencerCommandService
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.CoroutineScope

class CommonUtils(
        private val sequencerAdminFactory: SequencerAdminFactoryApi,
        private val locationServiceUtil: LocationServiceUtil,
        private val lockUnlockUtil: LockUnlockUtil,
        private val actorSystem: ActorSystem<*>,
        private val coroutineScope: CoroutineScope
) {
    private val timeout: Timeout = Timeout(Timeouts.DefaultTimeout())

    fun resolveAssembly(name: String): RichCommandService =
            RichCommandService(name, JComponentType.Assembly(), lockUnlockUtil, locationServiceUtil, actorSystem, timeout, coroutineScope)

    fun resolveHcd(name: String): RichCommandService =
            RichCommandService(name, JComponentType.HCD(), lockUnlockUtil, locationServiceUtil, actorSystem, timeout, coroutineScope)

    fun resolveSequencer(sequencerId: String, observingMode: String): RichSequencerCommandService =
            RichSequencerCommandService(sequencerId, observingMode, sequencerAdminFactory, locationServiceUtil, actorSystem)

}
