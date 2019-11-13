package esw.ocs.scripts.examples.testData

import com.typesafe.config.ConfigFactory
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
import scala.jdk.javaapi.CollectionConverters
import java.util.*

script {

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(InitialCommandHandler)

    onSetup("command-1") {
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }

    onSetup("command-2") {
    }

    onSetup("check-config") {
        if (existsConfig("/tmt/test/wfos.conf"))
            publishEvent(SystemEvent("WFOS", "check-config.success"))
    }

    onSetup("get-config-data") {
        val configValue = "component = wfos"
        val configData = getConfig("/tmt/test/wfos.conf")
        configData?.let {
            if (it == ConfigFactory.parseString(configValue))
                publishEvent(SystemEvent("WFOS", "get-config.success"))
        }
    }

    onSetup("command-3") {
    }

    onSetup("get-event") {
        // ESW-88
        val event: Event = getEvent("TCS.get.event").first()
        val successEvent = SystemEvent("TCS", "get.success")
        if (!event.isInvalid) publishEvent(successEvent)
    }

    onSetup("on-event") {
        onEvent("TCS.get.event") {
            val successEvent = SystemEvent("TCS", "onEvent.success")
            if (!it.isInvalid) publishEvent(successEvent)
        }
    }

    onSetup("command-for-assembly") { command ->
        submitCommandToAssembly("test", command)
    }

    onSetup("command-4") {
        // try sending concrete sequence
        val setupCommand = Setup(
                Prefix("TCS.test"),
                CommandName("command-3"),
                Optional.ofNullable(null)
        )
        val sequence = Sequence(
                Id("testSequenceIdString123"),
                CollectionConverters.asScala(Collections.singleton<SequenceCommand>(setupCommand)).toSeq()
        )

        // ESW-88, ESW-145, ESW-195
        submitSequence("tcs", "darknight", sequence)
    }

    onSetup("test-sequencer-hierarchy") {
        delay(5000)
    }

    onSetup("check-exception-1") {
        throw RuntimeException("boom")
    }

    onSetup("check-exception-2") {
    }

    onSetup("set-alarm-severity") {
        val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
        setSeverity(alarmKey, Major())
        delay(500)
    }

    onSetup("command-irms") {
        // NOT update command response to avoid sequencer to finish immediately
        // so that other Add, Append command gets time
        val setupCommand = setup("LGSF.test", "command-irms")
        val sequence = Sequence(
                Id("testSequenceIdString123"),
                CollectionConverters.asScala(Collections.singleton<SequenceCommand>(setupCommand)).toSeq()
        )

        submitSequence("lgsf", "darknight", sequence)
    }

    onDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        diagnosticModeForComponent("test", Assembly(), startTime, hint)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        operationsModeForComponent("test", Assembly())
    }

    onGoOffline {
        // do some actions to go offline
        goOfflineModeForComponent("test", Assembly())
    }

    onGoOnline {
        // do some actions to go online
        goOnlineModeForComponent("test", Assembly())
    }

    onAbortSequence {
        //do some actions to abort sequence

        //send abortSequence command to downstream sequencer
        abortSequenceForSequencer("lgsf", "darknight")
    }

    onStop {
        //do some actions to stop

        //send stop command to downstream sequencer
        stopSequencer("lgsf", "darknight")
    }

}
