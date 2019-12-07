package esw.ocs.scripts.examples.testData

import com.typesafe.config.ConfigFactory
import csw.alarm.api.javadsl.JAlarmSeverity.Major
import csw.alarm.models.Key.AlarmKey
import csw.params.events.Event
import csw.params.javadsl.JSubsystem.NFIRAOS
import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay
import kotlin.time.seconds

script {
    val lgsfSequencer = Sequencer("lgsf", "darknight")
    val testAssembly = Assembly("esw.test")

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
            publishEvent(SystemEvent("wfos.test", "check-config.success"))
    }

    onSetup("get-config-data") {
        val configValue = "component = wfos"
        val configData = getConfig("/tmt/test/wfos.conf")
        configData?.let {
            if (it == ConfigFactory.parseString(configValue))
                publishEvent(SystemEvent("wfos.test", "get-config.success"))
        }
    }

    onSetup("command-3") {
    }

    onSetup("get-event") {
        // ESW-88
        val event: Event = getEvent("esw.test.get.event").first()
        val successEvent = SystemEvent("esw.test", "get.success")
        if (!event.isInvalid) publishEvent(successEvent)
    }

    onSetup("on-event") {
        onEvent("esw.test.get.event") {
            val successEvent = SystemEvent("esw.test", "onEvent.success")
            if (!it.isInvalid) publishEvent(successEvent)
        }
    }

    onSetup("command-for-assembly") { command ->
        testAssembly.submit(command)
    }

    onSetup("command-4") {
        // try sending concrete sequence
        val setupCommand = Setup("esw.test", "command-3")
        val sequence = sequenceOf(setupCommand)

        // ESW-88, ESW-145, ESW-195
        val tcsSequencer = Sequencer("tcs", "darknight")
        tcsSequencer.submitAndWait(sequence, 10.seconds)
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
        val alarmKey = AlarmKey(NFIRAOS(), "trombone", "tromboneAxisHighLimitAlarm")
        setSeverity(alarmKey, Major())
        delay(500)
    }

    onSetup("command-lgsf") {
        // NOT update command response to avoid a sequencer to finish immediately
        // so that other Add, Append command gets time
        val setupCommand = Setup("lgsf.test", "command-lgsf")
        val sequence = sequenceOf(setupCommand)

        lgsfSequencer.submitAndWait(sequence, 10.seconds)
    }

    onDiagnosticMode { startTime, hint ->
        // do some actions to go to diagnostic mode based on hint
        testAssembly.diagnosticMode(startTime, hint)
    }

    onOperationsMode {
        // do some actions to go to operations mode
        testAssembly.operationsMode()
    }

    onGoOffline {
        // do some actions to go offline
        testAssembly.goOffline()
    }

    onGoOnline {
        // do some actions to go online
        testAssembly.goOnline()
    }

    onAbortSequence {
        //do some actions to abort sequence

        //send abortSequence command to downstream sequencer
        lgsfSequencer.abortSequence()
    }

    onStop {
        //do some actions to stop

        //send stop command to downstream sequencer
        lgsfSequencer.stop()
    }

}
