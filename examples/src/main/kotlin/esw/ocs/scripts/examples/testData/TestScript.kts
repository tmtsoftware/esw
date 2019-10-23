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
import kotlinx.coroutines.delay
import scala.Option
import scala.collection.immutable.HashSet
import scala.jdk.javaapi.CollectionConverters
import java.util.*

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
        // ESW-88
        val event: Event = getEvent("TCS.get.event").first()
        val successEvent = systemEvent("TCS", "get.success")
        if (!event.isInvalid) publishEvent(successEvent)
        addOrUpdateCommand(CommandResponse.Completed(it.runId()))
    }

    handleSetup("command-for-assembly") {command ->
        submitCommandToAssembly("test", command)
        addOrUpdateCommand(CommandResponse.Completed(command.runId()))
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

        // ESW-88, ESW-145, ESW-195
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
        setSeverity(alarmKey, Major())
        delay(500)
        addOrUpdateCommand(CommandResponse.Completed(command.runId()))
    }

    handleSetup("command-irms") { command ->
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        val setupCommand = Setup(
                Id("command-4-irms"),
                Prefix("IRMS.test"),
                CommandName("command-irms"),
                Option.apply(null),
                HashSet()
        )
        val sequence = Sequence(
                Id("testSequenceIdString123"),
                CollectionConverters.asScala(Collections.singleton<SequenceCommand>(setupCommand)).toSeq()
        )

        submitSequence("irms", "darknight", sequence)
    }

    handleDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        diagnosticModeForComponent("test", Assembly(), startTime, hint)
    }

    handleOperationsMode {
        // do some actions to go to operations mode
        operationsModeForComponent("test", Assembly())
    }

    handleGoOffline {
        // do some actions to go offline
        goOfflineModeForComponent("test", Assembly())
    }

    handleGoOnline {
        // do some actions to go online
        goOnlineModeForComponent("test", Assembly())
    }

    handleAbortSequence {
        //do some actions to abort sequence

        //send abortSequence command to downstream sequencer
        abortSequenceForSequencer("irms", "darknight")
    }

    handleStop {
        //do some actions to stop

        //send stop command to downstream sequencer
        stop("irms", "darknight")
    }
}
