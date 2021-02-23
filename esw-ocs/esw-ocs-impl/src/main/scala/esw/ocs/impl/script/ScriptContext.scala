package esw.ocs.impl.script

import java.time.Duration
import java.util.concurrent.CompletionStage

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.Config
import csw.alarm.api.javadsl.IAlarmService
import csw.event.api.javadsl.IEventService
import csw.logging.api.javadsl.ILogger
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.impl.core.SequenceOperator

class ScriptContext(
    val heartbeatInterval: Duration,
    val prefix: Prefix,
    val obsMode: ObsMode,
    val jLogger: ILogger,
    val sequenceOperatorFactory: () => SequenceOperator,
    val actorSystem: ActorSystem[SpawnProtocol.Command],
    val eventService: IEventService,
    val alarmService: IAlarmService,
    val sequencerApiFactory: (Subsystem, ObsMode) => CompletionStage[SequencerApi],
    val config: Config
)
