package esw.ocs.dsl2.highlevel

import akka.actor.typed.ActorSystem
import csw.location.api.javadsl.JComponentType
import csw.location.api.models.ComponentType
import csw.logging.api.scaladsl.Logger
import csw.params.events.SequencerObserveEvent
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.scheduler.api.TimeServiceScheduler
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.{CommandUtil, LockUnlockUtil}
import esw.ocs.dsl2.lowlevel.CswServices
import esw.ocs.impl.script.ScriptContext

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.jdk.DurationConverters.JavaDurationOps

class CswHighLevelDsl(
    val logger: Logger,
    cswServices: CswServices,
    locationServiceDsl: LocationServiceDsl,
    configServiceDsl: ConfigServiceDsl,
    eventServiceDsl: EventServiceDsl,
    commandServiceDsl: CommandServiceDsl,
    alarmServiceDsl: AlarmServiceDsl,
    timeServiceScheduler: TimeServiceScheduler,
    databaseServiceDsl: DatabaseServiceDsl,
    val loopDsl: LoopDsl,
    scriptContext: ScriptContext,
    val strandEc: StrandEc,
    sequencerObserveEvent: SequencerObserveEvent
) {
  export sequencerObserveEvent.*
  export cswServices.*
  export locationServiceDsl.*
  export configServiceDsl.*
  export eventServiceDsl.*
  export commandServiceDsl.*
  export alarmServiceDsl.*
  export timeServiceScheduler.*
  export databaseServiceDsl.*
  export loopDsl.*

  import cswServices.locationService

  given ActorSystem[_]   = scriptContext.actorSystem
  given ExecutionContext = strandEc.ec

  private lazy val locationServiceUtil: LocationServiceUtil = new LocationServiceUtil(locationService)
  private lazy val commandUtil: CommandUtil                 = new CommandUtil(locationServiceUtil)
  private lazy val lockUnlockUtil: LockUnlockUtil           = new LockUnlockUtil(scriptContext.prefix)(scriptContext.actorSystem)

  private val alarmConfig             = scriptContext.config.getConfig("csw-alarm")
  val _alarmRefreshDuration: Duration = alarmConfig.getDuration("refresh-interval").toScala

  private def richComponent(prefix: Prefix, componentType: ComponentType, defaultTimeout: FiniteDuration): RichComponent =
    new RichComponent(prefix, componentType, lockUnlockUtil, commandUtil, defaultTimeout)

  private def richSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Variation,
      defaultTimeout: FiniteDuration
  ): RichSequencer =
    new RichSequencer(
      subsystem,
      obsMode,
      variation,
      (s, o, v) => scriptContext.sequencerApiFactory(s, o, Option.apply(v)),
      defaultTimeout
    )

  def Assembly(prefix: Prefix, defaultTimeout: FiniteDuration): RichComponent =
    richComponent(prefix, ComponentType.Assembly, defaultTimeout)
  def Hcd(prefix: Prefix, defaultTimeout: FiniteDuration): RichComponent =
    richComponent(prefix, JComponentType.HCD, defaultTimeout)

  def Sequencer(subsystem: Subsystem, obsMode: ObsMode): RichSequencer =
    richSequencer(subsystem, obsMode, null, 10.hours)

  def Sequencer(subsystem: Subsystem, obsMode: ObsMode, defaultTimeout: FiniteDuration): RichSequencer =
    richSequencer(subsystem, obsMode, null, defaultTimeout)

  def Sequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation): RichSequencer =
    richSequencer(subsystem, obsMode, variation, 10.hours)

  def Sequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation, defaultTimeout: FiniteDuration): RichSequencer =
    richSequencer(subsystem, obsMode, variation, defaultTimeout)

  def Assembly(prefix: Prefix): RichComponent = Assembly(prefix, 10.seconds)

  def Assembly(subsystem: Subsystem, compName: String, defaultTimeout: FiniteDuration = 10.seconds): RichComponent =
    Assembly(Prefix(subsystem, compName), defaultTimeout)

  def Hcd(prefix: Prefix): RichComponent = Hcd(prefix, 10.seconds)
  def Hcd(subsystem: Subsystem, compName: String, defaultTimeout: FiniteDuration = 10.seconds): RichComponent =
    Hcd(Prefix(subsystem, compName), defaultTimeout)

  def finishWithError(message: String = ""): Nothing = throw RuntimeException(message)
}
