package esw.ocs.dsl.highlevel

import csw.location.api.models.ComponentType
import csw.params.core.generics.Key
import csw.params.events.EventKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.epics.*
import esw.ocs.dsl.highlevel.models.Assembly
import esw.ocs.dsl.highlevel.models.HCD
import esw.ocs.dsl.lowlevel.CswServices
import esw.ocs.dsl.params.SequencerObserveEvent
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.CommandUtil
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.hours
import kotlin.time.seconds
import kotlin.time.toKotlinDuration

interface CswHighLevelDslApi : CswServices, LocationServiceDsl, ConfigServiceDsl, EventServiceDsl, LoggingDsl, CommandServiceDsl,
        AlarmServiceDsl, TimeServiceDsl, DatabaseServiceDsl, LoopDsl {
    val isOnline: Boolean
    val prefix: String
    val sequencerObserveEvent: SequencerObserveEvent

    fun Assembly(prefix: Prefix, defaultTimeout: Duration = 10.seconds): RichComponent
    fun Assembly(subsystem: Subsystem, compName: String, defaultTimeout: Duration = 10.seconds): RichComponent =
            Assembly(Prefix(subsystem, compName), defaultTimeout)

    fun Hcd(prefix: Prefix, defaultTimeout: Duration = 10.seconds): RichComponent
    fun Hcd(subsystem: Subsystem, compName: String, defaultTimeout: Duration = 10.seconds): RichComponent =
            Hcd(Prefix(subsystem, compName), defaultTimeout)

    fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, defaultTimeout: Duration = 10.hours): RichSequencer

    suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm
    fun commandFlag(): CommandFlag

    /**
     * Method to create an instance of [[esw.ocs.dsl.epics.ParamVariable]] tied to the particular param `key` of an [[csw.params.events.Event]]
     * being published on specific `event key`.
     *
     * [[esw.ocs.dsl.epics.ParamVariable]] is [[esw.ocs.dsl.epics.EventVariable]] with methods to get and set a specific parameter in the [[csw.params.events.Event]]
     *
     * It behaves differently depending on the presence of `duration` parameter while creating its instance.
     * - When provided with `duration`, it will **poll** at an interval of given `duration` to refresh its own value
     * - Otherwise it will **subscribe** to the given event key and will refresh its own value whenever events are published
     *
     * @param initial value to set to the parameter key of the event
     * @param eventKeyStr string representation of event key
     * @param key represents parameter key of the event to tie [[esw.ocs.dsl.epics.ParamVariable]] to
     * @param duration represents the interval of polling.
     * @return instance of [[esw.ocs.dsl.epics.ParamVariable]]
     */
    suspend fun <T> ParamVariable(initial: T, eventKeyStr: String, key: Key<T>, duration: Duration? = null): ParamVariable<T> =
            ParamVariable.make(initial, key, EventKey.apply(eventKeyStr), this, duration)

    /**
     * Method to create an instance of [[esw.ocs.dsl.epics.EventVariable]] tied to an [[csw.params.events.Event]] being published on specified `event key`.
     *
     * [[esw.ocs.dsl.epics.EventVariable]] behaves differently depending on the presence of `duration` parameter while creating its instance.
     * - When provided with `duration`, it will **poll** at an interval of given `duration` to refresh its own value
     * - Otherwise it will **subscribe** to the given event key and will refresh its own value whenever events are published
     *
     * @param eventKeyStr string representation of event key
     * @param duration represents the interval of polling.
     * @ return instance of [[esw.ocs.dsl.epics.EventVariable]]
     */
    suspend fun EventVariable(eventKeyStr: String, duration: Duration? = null): EventVariable =
            EventVariable.make(EventKey.apply(eventKeyStr), this, duration)

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)
}

abstract class CswHighLevelDsl(private val cswServices: CswServices, private val scriptContext: ScriptContext) : CswHighLevelDslApi, CswServices by cswServices {
    abstract val strandEc: StrandEc
    abstract override val coroutineScope: CoroutineScope

    private val locationServiceUtil: LocationServiceUtil by lazy { LocationServiceUtil(locationService.asScala(), actorSystem) }
    private val commandUtil: CommandUtil by lazy { CommandUtil(locationServiceUtil, actorSystem) }
    private val lockUnlockUtil: LockUnlockUtil by lazy { LockUnlockUtil(scriptContext.prefix(), actorSystem) }

    private val alarmConfig = scriptContext.config().getConfig("csw-alarm")
    override val _alarmRefreshDuration: Duration = alarmConfig.getDuration("refresh-interval").toKotlinDuration()

    /******** Command Service helpers ********/
    private fun richComponent(prefix: Prefix, componentType: ComponentType, defaultTimeout: Duration): RichComponent =
            RichComponent(prefix, componentType, lockUnlockUtil, commandUtil, actorSystem, defaultTimeout, coroutineScope)

    private fun richSequencer(subsystem: Subsystem, obsMode: ObsMode, defaultTimeout: Duration): RichSequencer =
            RichSequencer(subsystem, obsMode, { s, o -> scriptContext.sequencerApiFactory().apply(s, o) }, defaultTimeout, coroutineScope)

    override fun Assembly(prefix: Prefix, defaultTimeout: Duration): RichComponent = richComponent(prefix, Assembly, defaultTimeout)
    override fun Hcd(prefix: Prefix, defaultTimeout: Duration): RichComponent = richComponent(prefix, HCD, defaultTimeout)
    override fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, defaultTimeout: Duration): RichSequencer = richSequencer(subsystem, obsMode, defaultTimeout)

    /************* Fsm helpers **********/
    override suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm =
            FsmImpl(name, initState, coroutineScope, this).apply { block() }

    override fun commandFlag(): CommandFlag = CommandFlag()

}
