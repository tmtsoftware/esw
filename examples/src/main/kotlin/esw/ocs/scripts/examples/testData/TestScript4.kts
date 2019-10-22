package esw.ocs.scripts.examples.testData

import csw.alarm.api.javadsl.JAlarmSeverity
import csw.alarm.models.Key
import csw.params.commands.CommandResponse
import csw.params.javadsl.JSubsystem
import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    handleSetup("command-irms") { command ->
        // To avoid sequencer to finish immediately so that other commands gets time
        delay(500)
        addOrUpdateCommand(CommandResponse.Completed(command.runId))
    }

    handleAbortSequence {
        //do some actions to abort sequence
        val alarmKey = Key.AlarmKey(JSubsystem.IRMS, "irmsSequencer", "alarmAbort")
        setSeverity(alarmKey, JAlarmSeverity.Major())
    }

    handleStop {
        //do some actions to stop
        val alarmKey = Key.AlarmKey(JSubsystem.IRMS, "irmsSequencer", "alarmStop")
        setSeverity(alarmKey, JAlarmSeverity.Major())
    }
}
