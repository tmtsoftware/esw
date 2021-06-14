package esw.ocs.scripts.examples.testData

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.intKey
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

script {

    val pollingVar = ParamVariable(0, "TCS.polling.param-var-test", intKey("counter"), Duration.milliseconds(400))
    val paramVarFsm = Fsm("param-pollingTest", "INIT") {
        state("INIT") {
            val event = SystemEvent("TCS.polling", "param-var-test")
            publishEvent(event)
        }
    }

    pollingVar.bind(paramVarFsm)

    onSetup("command-1") {
        // To avoid sequencer to finish immediately so that other Add, Append command gets time
        delay(200)
    }

    onSetup("command-2") {
    }

    onSetup("command-3") {
    }

    onSetup("command-4") {
        //Don't complete immediately as this is used to abort sequence usecase
        delay(700)
    }

    onSetup("command-5") {
    }

    onSetup("command-6") {
    }

    onSetup("fail-command") { command ->
        finishWithError(command.commandName().name())
    }

    onSetup("multi-node") { command ->
        val sequence = sequenceOf(command)

        val tcs = Sequencer(TCS, ObsMode("moonnight"), Duration.seconds(10))
        tcs.submitAndWait(sequence, Duration.seconds(10))
    }

    onSetup("log-command") {
        fatal("log-message")
    }

    onSetup("start-param-fsm") {
        paramVarFsm.start()
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
        InitialCommandHandler,
        OnlineOfflineHandlers,
        OperationsAndDiagModeHandlers
    )
}
