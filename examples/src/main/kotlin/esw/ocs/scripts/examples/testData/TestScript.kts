package esw.ocs.scripts.examples.testData

import csw.alarm.api.javadsl.JAlarmSeverity.Major
import csw.alarm.models.Key.AlarmKey
import csw.location.api.javadsl.JComponentType.Assembly
import csw.params.commands.*
import csw.params.commands.CommandResponse.*
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

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(InitialCommandHandler)

    handleSetup("command-1") { command ->
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }

    handleSetup("command-2") { command ->
    }

    handleSetup("check-config") { command ->
        if (existsConfig("/tmt/test/wfos.conf"))
            publishEvent(systemEvent("WFOS", "config.success"))
    }

    handleSetup("get-config-data") { command ->
        val configValue = "component = wfos"
        val configData = getConfig("/tmt/test/wfos.conf")
        configData?.let {
            if (it == configValue)
                publishEvent(systemEvent("WFOS", "config.success"))
        }
    }

    handleSetup("command-3") { command ->
    }

    handleSetup("get-event") {
        // ESW-88
        val event: Event = getEvent("TCS.get.event").first()
        val successEvent = systemEvent("TCS", "get.success")
        if (!event.isInvalid) publishEvent(successEvent)
    }

    handleSetup("command-for-assembly") { command ->
        submitCommandToAssembly("test", command)
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
    }

    handleSetup("test-sequencer-hierarchy") {
        delay(5000)
    }

    handleSetup("fail-command") { command ->
    }

    handleSetup("check-exception-1") { command ->
        throw RuntimeException("boom")
    }

    handleSetup("check-exception-2") { command ->
    }

    handleSetup("set-alarm-severity") { command ->
        val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
        setSeverity(alarmKey, Major())
        delay(500)
    }

    handleSetup("command-irms") { _ ->
        // NOT update command response to avoid sequencer to finish immediately
        // so that other Add, Append command gets time
        val setupCommand = Setup(
                Prefix("IRMS.test"),
                CommandName("command-irms"),
                Optional.ofNullable(null)
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
