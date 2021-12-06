package esw.ocs.dsl.highlevel

import csw.location.api.models.ComponentType
import csw.params.core.generics.Key
import csw.params.core.models.ExposureId
import csw.params.core.models.ObsId
import csw.params.events.EventKey
import csw.params.events.ObserveEvent
import csw.params.events.SequencerObserveEvent
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.api.models.Variation
import esw.ocs.dsl.epics.*
import esw.ocs.dsl.highlevel.models.Assembly
import esw.ocs.dsl.highlevel.models.HCD
import esw.ocs.dsl.lowlevel.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.CommandUtil
import esw.ocs.dsl.script.utils.LockUnlockUtil
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.CoroutineScope
import scala.Option
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

/**
 * Interface which contains methods to create different observe events by delegating to ScalaDsl of creating observe events
 * and has abstract methods to create FSM, CommandFlag, command service for Sequencer, Assembly and Hcd
 *
 */
interface CswHighLevelDslApi : CswServices, LocationServiceDsl, ConfigServiceDsl, EventServiceDsl, LoggingDsl, CommandServiceDsl,
        AlarmServiceDsl, TimeServiceDsl, DatabaseServiceDsl, LoopDsl {
    val isOnline: Boolean
    val prefix: String
    val obsMode: ObsMode
    val sequencerObserveEvent: SequencerObserveEvent

    /**
     * This event indicates the start of the preset phase of acquisition
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun presetStart(obsId: ObsId): ObserveEvent = sequencerObserveEvent.presetStart(obsId)

    /**
     * This event indicates the end of the preset phase of  acquisition
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun presetEnd(obsId: ObsId): ObserveEvent = sequencerObserveEvent.presetEnd(obsId)

    /**
     * This event indicates the start of locking the telescope to the  sky with guide and WFS targets
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun guidestarAcqStart(obsId: ObsId): ObserveEvent = sequencerObserveEvent.guidestarAcqStart(obsId)

    /**
     * This event indicates the end of locking the telescope to the sky with guide and WFS targets
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun guidestarAcqEnd(obsId: ObsId): ObserveEvent = sequencerObserveEvent.guidestarAcqEnd(obsId)

    /**
     * This event indicates the start of acquisition phase where  science target is peaked up as needed after  guidestar locking
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun scitargetAcqStart(obsId: ObsId): ObserveEvent = sequencerObserveEvent.scitargetAcqStart(obsId)

    /**
     * This event indicates the end of acquisition phase where  science target is centered as needed after  guidestar locking
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun scitargetAcqEnd(obsId: ObsId): ObserveEvent = sequencerObserveEvent.scitargetAcqEnd(obsId)

    /**
     * This event indicates the start of execution of actions related  to an observation including acquisition and  science data acquisition.
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun observationStart(obsId: ObsId): ObserveEvent = sequencerObserveEvent.observationStart(obsId)

    /**
     * This event indicates the end of execution of actions related  to an observation including acquisition and  science data acquisition.
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun observationEnd(obsId: ObsId): ObserveEvent = sequencerObserveEvent.observationEnd(obsId)

    /**
     * This event indicates the start of execution of actions related  to an Observe command
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun observeStart(obsId: ObsId): ObserveEvent = sequencerObserveEvent.observeStart(obsId)

    /**
     * This event indicates the end of execution of actions related  to an Observe command
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun observeEnd(obsId: ObsId): ObserveEvent = sequencerObserveEvent.observeEnd(obsId)

    /**
     * This event indicates the start of data acquisition that  results in a file produced for DMS. This is a potential metadata event for DMS.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun exposureStart(exposureId: ExposureId): ObserveEvent = sequencerObserveEvent.exposureStart(exposureId)

    /**
     * This event indicates the end of data acquisition that results  in a file produced for DMS. This is a potential metadata event for DMS.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun exposureEnd(exposureId: ExposureId): ObserveEvent = sequencerObserveEvent.exposureEnd(exposureId)

    /**
     * This event indicates that a readout that is part of a ramp  has completed.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun readoutEnd(exposureId: ExposureId): ObserveEvent = sequencerObserveEvent.readoutEnd(exposureId)

    /**
     * This event indicates that a readout that is part of a ramp  has failed indicating transfer failure or some  other issue.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun readoutFailed(exposureId: ExposureId): ObserveEvent = sequencerObserveEvent.readoutFailed(exposureId)

    /**
     * This event indicates that the instrument has started writing  the exposure data file or transfer of exposure  data to DMS.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @param filename   [[java.lang.String]] the path of the file.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun dataWriteStart(exposureId: ExposureId, filename: String): ObserveEvent = sequencerObserveEvent.dataWriteStart(exposureId, filename)

    /**
     * This event indicates that the instrument has finished  writing the exposure data file or transfer of  exposure data to DMS.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @param filename   [[java.lang.String]] the path of the file.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun dataWriteEnd(exposureId: ExposureId, filename: String): ObserveEvent = sequencerObserveEvent.dataWriteEnd(exposureId, filename)

    /**
     * This event indicates the start of data acquisition that  results in a file produced for DMS. This is a potential metadata event for DMS.
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun prepareStart(exposureId: ExposureId): ObserveEvent = sequencerObserveEvent.prepareStart(exposureId)

    /**
     * This event indicates that a request was made to abort the  exposure and it has completed. Normal data events should occur if data is  recoverable.
     * Abort should not fail
     * @param exposureId [[csw.params.core.models.ExposureId]] is an identifier in ESW/DMS for a single exposure.
     *                   The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or
     *                   when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time
     *                   when the ExposureId is created.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun exposureAborted(exposureId: ExposureId): ObserveEvent = sequencerObserveEvent.observePaused()

    /**
     * This event indicates that a user has paused the current  observation Sequence which will happen after  the current step concludes
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun observePaused(): ObserveEvent = sequencerObserveEvent.observePaused()

    /**
     * This event indicates that a user has resumed a paused  observation Sequence.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun observeResumed(): ObserveEvent = sequencerObserveEvent.observeResumed()

    /**
     * This event indicates that something has occurred that  interrupts the normal observing workflow and  time accounting.
     * This event will have a hint (TBD) that indicates  the cause of the downtime for statistics.
     * Examples are: weather, equipment or other  technical failure, etc.
     * Downtime is ended by the start of an observation  or exposure.
     * @param obsId [[csw.params.core.models.ObsId]] Represents a unique observation id
     * @param reasonForDowntime [[java.lang.String]] a hint that indicates the cause of the downtime for statistics.
     * @return [[csw.params.events.ObserveEvent]]
     */
    fun downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = sequencerObserveEvent.downtimeStart(obsId, reasonForDowntime)

    /**
     * Creates an instance of RichComponent for Assembly of given prefix
     *
     * @param prefix - prefix of Assembly
     * @param defaultTimeout - default timeout for the response of the RichComponent's API
     *
     * @return a [[esw.ocs.dsl.highlevel.RichComponent]] instance
     */
    fun Assembly(prefix: Prefix, defaultTimeout: Duration = Duration.seconds(10)): RichComponent
    fun Assembly(subsystem: Subsystem, compName: String, defaultTimeout: Duration = Duration.seconds(10)): RichComponent =
            Assembly(Prefix(subsystem, compName), defaultTimeout)

    /**
     * Creates an instance of RichComponent for HCD of given prefix
     *
     * @param prefix - prefix of HCD
     * @param defaultTimeout - default timeout for the response of the RichComponent's API
     *
     * @return a [[esw.ocs.dsl.highlevel.RichComponent]] instance
     */
    fun Hcd(prefix: Prefix, defaultTimeout: Duration = Duration.seconds(10)): RichComponent

    fun Hcd(subsystem: Subsystem, compName: String, defaultTimeout: Duration = Duration.seconds(10)): RichComponent =
            Hcd(Prefix(subsystem, compName), defaultTimeout)

    /**
     * Creates an instance of RichSequencer for Sequencer of given subsystem and obsMode
     *
     * @param subsystem - Subsystem of the sequencer
     * @param obsMode - ObsMode of the sequencer
     * @return a [[esw.ocs.dsl.highlevel.RichSequencer]] instance
     */
    fun Sequencer(subsystem: Subsystem, obsMode: ObsMode): RichSequencer

    /**
     * Creates an instance of RichSequencer for Sequencer of given subsystem and obsMode
     *
     * @param subsystem - Subsystem of the sequencer
     * @param obsMode - ObsMode of the sequencer
     * @param defaultTimeout - default timeout for the response of the RichSequencer's API
     * @return a [[esw.ocs.dsl.highlevel.RichSequencer]] instance
     */
    fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, defaultTimeout: Duration): RichSequencer

    /**
     * Creates an instance of RichSequencer for Sequencer of given subsystem and obsMode
     *
     * @param subsystem - Subsystem of the sequencer
     * @param obsMode - ObsMode of the sequencer
     * @param variation - variation of the sequencer
     * @return a [[esw.ocs.dsl.highlevel.RichSequencer]] instance
     */
    fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation): RichSequencer

    /**
     * Creates an instance of RichSequencer for Sequencer of given subsystem and obsMode
     *
     * @param subsystem - Subsystem of the sequencer
     * @param obsMode - ObsMode of the sequencer
     * @param variation - variation of the sequencer
     * @param defaultTimeout - default timeout for the response of the RichSequencer's API
     * @return a [[esw.ocs.dsl.highlevel.RichSequencer]] instance
     */
    fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation, defaultTimeout: Duration): RichSequencer

    /**
     * TODO
     *
     * @param name
     * @param initState
     * @param block
     * @return
     */
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

    private fun richSequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation?, defaultTimeout: Duration): RichSequencer =
            RichSequencer(subsystem, obsMode, variation, { s, o, v -> scriptContext.sequencerApiFactory().apply(s, o, Option.apply(v)) }, defaultTimeout, coroutineScope)

    override fun Assembly(prefix: Prefix, defaultTimeout: Duration): RichComponent = richComponent(prefix, Assembly, defaultTimeout)
    override fun Hcd(prefix: Prefix, defaultTimeout: Duration): RichComponent = richComponent(prefix, HCD, defaultTimeout)

    override fun Sequencer(subsystem: Subsystem, obsMode: ObsMode): RichSequencer = richSequencer(subsystem, obsMode, null, Duration.hours(10))

    override fun Sequencer(subsystem: Subsystem, obsMode: ObsMode,  defaultTimeout: Duration): RichSequencer = richSequencer(subsystem, obsMode, null, defaultTimeout)

    override fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation): RichSequencer = richSequencer(subsystem, obsMode, variation, Duration.hours(10))

    override fun Sequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation , defaultTimeout: Duration): RichSequencer =
            richSequencer(subsystem, obsMode, variation, defaultTimeout)

    /************* Fsm helpers **********/
    override suspend fun Fsm(name: String, initState: String, block: suspend FsmScope.() -> Unit): Fsm =
            FsmImpl(name, initState, coroutineScope, this).apply { block() }

    override fun commandFlag(): CommandFlag = CommandFlag()

}
