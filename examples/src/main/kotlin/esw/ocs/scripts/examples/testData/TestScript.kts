/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData

import com.typesafe.config.ConfigFactory
import csw.alarm.models.Key.AlarmKey
import csw.params.commands.CommandResponse
import csw.params.core.states.StateName
import csw.params.events.Event
import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.*
import esw.ocs.dsl.params.longKey
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

script {
    val lgsfSequencer = Sequencer(LGSF, ObsMode("darknight"), 10.seconds)
    val testAssembly = Assembly(ESW, "test", 10.seconds)

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(InitialCommandHandler)

    onSetup("command-2") {
    }

    // ESW-421 demonstrate creating exposureId and obsId. Getting components from exposureId and ObsId
    onObserve("exposure-start") {
        val obsId = ObsId("2021A-011-153")
        // do something with ObsId components
        println(obsId.programId)
        println(obsId.programId.semesterId)
        println(obsId.programId.semesterId.semester)

        // create exposureId
        val exposureIdStr = "${obsId}-TCS-DET-SCI0-0001"
        val exposureId = ExposureId(exposureIdStr)
        // do something with exposureId components
        println(exposureId.subsystem)
        println(exposureId.det)

        publishEvent(exposureStart(exposureId))
    }

    onSetup("command-3") {
    }

    onSetup("command-4") {
        // try sending concrete sequence
        val setupCommand = Setup("ESW.test", "command-3")
        val sequence = sequenceOf(setupCommand)

        // ESW-88, ESW-145, ESW-195
        val tcsSequencer = Sequencer(TCS, ObsMode("darknight"), 10.seconds)
        tcsSequencer.submitAndWait(sequence, 10.seconds)
    }

    onSetup("check-config") {
        if (existsConfig("/tmt/test/wfos.conf"))
            publishEvent(SystemEvent("WFOS.test", "check-config.success"))
    }

    onSetup("get-config-data") {
        val configValue = "component = wfos"
        val configData = getConfig("/tmt/test/wfos.conf")
        configData?.let {
            if (it == ConfigFactory.parseString(configValue))
                publishEvent(SystemEvent("WFOS.test", "get-config.success"))
        }
    }

    onSetup("get-event") {
        // ESW-88
        val event: Event = getEvent("ESW.test.get.event")
        val successEvent = SystemEvent("ESW.test", "get.success")
        if (!event.isInvalid) publishEvent(successEvent)
    }

    onSetup("on-event") {
        onEvent("ESW.test.get.event") {
            val successEvent = SystemEvent("ESW.test", "onevent.success")
            if (!it.isInvalid) publishEvent(successEvent)
        }
    }

    onSetup("command-for-assembly") { command ->
        val submitResponse = testAssembly.submit(Setup(command.source().toString(), "long-running"))

        when (testAssembly.query(submitResponse.runId())) {
            is CommandResponse.Started -> publishEvent(SystemEvent("tcs.filter.wheel", "query-started-command-from-script"))
        }

        when (testAssembly.queryFinal(submitResponse.runId(), 100.milliseconds)) {
            is CommandResponse.Completed -> publishEvent(SystemEvent("tcs.filter.wheel", "query-completed-command-from-script"))
        }

        testAssembly.subscribeCurrentState(StateName("stateName1"), StateName("stateName2")) { currentState ->
            publishEvent(SystemEvent("tcs.filter.wheel", "publish-${currentState.stateName().name()}"))
        }

        testAssembly.oneway(command)
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
        val alarmKey = AlarmKey(Prefix("NFIRAOS.trombone"), "tromboneAxisHighLimitAlarm")
        setSeverity(alarmKey, Major)
        delay(500)
    }

    onSetup("command-lgsf") {
        // NOT update command response to avoid a sequencer to finish immediately
        // so that others Add, Append command gets time
        val setupCommand = Setup("LGSF.test", "command-lgsf")
        val sequence = sequenceOf(setupCommand)

        lgsfSequencer.submitAndWait(sequence, 10.seconds)
    }

    onSetup("schedule-once-from-now") {
        val currentTime = utcTimeNow()
        scheduleOnceFromNow(1.seconds) {
            val param = longKey("offset").set(currentTime.offsetFromNow().absoluteValue.inWholeMilliseconds)
            publishEvent(SystemEvent("ESW.schedule.once", "offset", param))
        }
    }

    onSetup("schedule-periodically-from-now") {
        val currentTime = utcTimeNow()
        var counter = 0
        val a = schedulePeriodicallyFromNow(1.seconds, 1.seconds) {
            val param = longKey("offset").set(currentTime.offsetFromNow().absoluteValue.inWholeMilliseconds)
            publishEvent(SystemEvent("ESW.schedule.periodically", "offset", param))
            counter += 1
        }
        loop {
            stopWhen(counter > 1)
        }
        a.cancel()
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
