package esw.ocs.scripts.examples.testData

import csw.alarm.api.javadsl.JAlarmSeverity.Major
import csw.alarm.models.Key.AlarmKey
import csw.location.api.javadsl.JComponentType.Assembly
import csw.params.commands.*
import csw.params.core.models.Id
import csw.params.core.models.Prefix
import csw.params.events.Event
import csw.params.javadsl.JSubsystem.NFIRAOS
import esw.ocs.dsl.core.script
import java.util.*
import kotlinx.coroutines.delay
import scala.Option
import scala.collection.immutable.HashSet
import scala.jdk.javaapi.CollectionConverters

script {

    handleSetup("command-1") { command ->
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
        addOrUpdateCommand(CommandResponse.Completed(command.runId))
    }

    handleSetup("command-2") { command ->
        addOrUpdateCommand(CommandResponse.Completed(command.runId))
    }

    handleSetup("command-3") { command ->
        addOrUpdateCommand(CommandResponse.Completed(command.runId))
    }

    handleSetup("get-event") {
        val event: Event = getEvent("TCS.get.event").elementAt(0)
        val successEvent = systemEvent("TCS", "get.success")
        if (!event.isInvalid) publishEvent(successEvent)
        addOrUpdateCommand(CommandResponse.Completed(it.runId()))
    }

    handleSetup("command-4") { command ->
        // try sending concrete sequence
        val setupCommand = Setup(
                Id("testCommandIdString123"),
                Prefix("TCS.test"),
                CommandName("command-3"),
                Option.apply(null),
                HashSet()
        )
        val sequence = Sequence(
                Id("testSequenceIdString123"),
                CollectionConverters.asScala(Collections.singleton<SequenceCommand>(setupCommand)).toSeq()
        )

        // ESW-145, ESW-195
        submitSequence("tcs", "darknight", sequence)
        addOrUpdateCommand(CommandResponse.Completed(command.runId()))
    }

    handleSetup("test-sequencer-hierarchy") {
        delay(5000)
        addOrUpdateCommand(CommandResponse.Completed(it.runId()))
    }

    handleSetup("fail-command") { command ->
        addOrUpdateCommand(CommandResponse.Error(command.runId(), command.commandName().name()))
    }

    handleSetup("set-alarm-severity") { command ->
        val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
        setSeverity(alarmKey, Major)
        delay(500)
        addOrUpdateCommand(CommandResponse.Completed(command.runId()))
    }

    handleDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        diagnosticModeForComponent("test", Assembly, startTime, hint)
    }

    handleOperationsMode {
        // do some actions to go to operations mode
        operationsModeForComponent("test", Assembly)
    }

    handleGoOffline {
        // do some actions to go offline
        goOfflineModeForComponent("test", Assembly)
    }

    handleGoOnline {
        // do some actions to go online
        goOnlineModeForComponent("test", Assembly)
    }
}
