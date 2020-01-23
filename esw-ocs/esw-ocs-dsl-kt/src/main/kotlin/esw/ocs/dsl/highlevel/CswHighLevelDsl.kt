package esw.ocs.dsl.highlevel

import csw.location.models.ComponentType
import csw.prefix.models.Subsystem
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.Fsm
import esw.ocs.dsl.epics.FsmImpl
import esw.ocs.dsl.epics.FsmScope
import esw.ocs.dsl.highlevel.models.HCD
import esw.ocs.dsl.highlevel.models.Assembly
import esw.ocs.dsl.highlevel.models.Prefix
import esw.ocs.dsl.lowlevel.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.dsl.script.utils.SubsystemFactory
import esw.ocs.impl.internal.LocationServiceUtil
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

interface CswHighLevelDslApi : CswServices, LocationServiceDsl, ConfigServiceDsl, EventServiceDsl, LoggingDsl, CommandServiceDsl,
        AlarmServiceDsl, TimeServiceDsl, DatabaseServiceDsl, LoopDsl {

    fun Assembly(prefix: String, defaultTimeout: Duration): RichComponent
    fun Hcd(prefix: String, defaultTimeout: Duration): RichComponent
    fun Sequencer(subsystem: String, observingMode: String, defaultTimeout: Duration): RichSequencer

    suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm
    fun commandFlag(): CommandFlag

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)
}

abstract class CswHighLevelDsl(private val cswServices: CswServices, private val scriptContext: ScriptContext) : CswHighLevelDslApi, CswServices by cswServices {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    private val locationServiceUtil: LocationServiceUtil by lazy { LocationServiceUtil(locationService.asScala(), actorSystem) }
    private val lockUnlockUtil: LockUnlockUtil by lazy { LockUnlockUtil(scriptContext.prefix(), actorSystem) }

    private val alarmConfig = scriptContext.config().getConfig("csw-alarm")
    override val _alarmRefreshDuration: Duration = alarmConfig.getDuration("refresh-interval").toKotlinDuration()

    /******** Command Service helpers ********/
    private fun richComponent(prefix: String, componentType: ComponentType, defaultTimeout: Duration): RichComponent =
            RichComponent(Prefix(prefix), componentType, lockUnlockUtil, locationServiceUtil, actorSystem, defaultTimeout, coroutineScope)

    private fun richSequencer(subsystem: Subsystem, observingMode: String, defaultTimeout: Duration): RichSequencer =
            RichSequencer(subsystem, observingMode, scriptContext.sequencerApiFactory(), defaultTimeout, coroutineScope)

    override fun Assembly(prefix: String, defaultTimeout: Duration): RichComponent = richComponent(prefix, Assembly, defaultTimeout)
    override fun Hcd(prefix: String, defaultTimeout: Duration): RichComponent = richComponent(prefix, HCD, defaultTimeout)
    override fun Sequencer(subsystem: String, observingMode: String, defaultTimeout: Duration): RichSequencer = richSequencer(SubsystemFactory.make(subsystem), observingMode, defaultTimeout)

    /************* Fsm helpers **********/
    override suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm =
            FsmImpl(name, initState, coroutineScope, this).apply { block() }

    override fun commandFlag(): CommandFlag = CommandFlag()

}
