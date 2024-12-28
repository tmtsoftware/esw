package esw.ocs.impl.script

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.Config
import csw.alarm.api.javadsl.IAlarmService
import csw.event.api.javadsl.IEventService
import csw.logging.api.javadsl.ILogger
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.impl.core.SequenceOperator

import java.time.Duration
import java.util.concurrent.CompletionStage

/**
 * A context class created to pass following states to sequencer script
 *
 * @param heartbeatInterval - heart beat interval for health check
 * @param prefix - prefix of the sequencer
 * @param obsMode - obsMode of the sequencer
 * @param jLogger - java typed Logger
 * @param sequenceOperatorFactory - sequenceOperatorFactory
 * @param actorSystem - An Pekko ActorSystem
 * @param eventService - an java instance of EventService
 * @param alarmService - an java instance of AlarmService
 * @param sequencerApiFactory - a Factory method to create an instance of sequencerApi
 * @param config - overall config
 */
class ScriptContext(
    val heartbeatInterval: Duration,
    val prefix: Prefix,
    val obsMode: ObsMode,
    val jLogger: ILogger,
    val sequenceOperatorFactory: () => SequenceOperator,
    val actorSystem: ActorSystem[SpawnProtocol.Command],
    val eventService: IEventService,
    val alarmService: IAlarmService,
    val sequencerApiFactory: (Subsystem, ObsMode, Option[Variation]) => CompletionStage[SequencerApi],
    val config: Config
)
