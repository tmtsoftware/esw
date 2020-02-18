package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.intKey
import kotlinx.coroutines.delay
import kotlin.time.milliseconds
import kotlin.time.seconds

script {

    val pollingVar = ParamVariable(0, "TCS.polling.test", intKey("counter"), 400.milliseconds)

    val fsm = Fsm("pollingTest", "INIT") {
        state("INIT") {
            val event = SystemEvent("TCS.polling", "test")
            publishEvent(event)
        }
    }
    pollingVar.bind(fsm)

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

        val tcs = Sequencer(TCS, "moonnight", 10.seconds)
        tcs.submitAndWait(sequence, 10.seconds)
    }

    onSetup("log-command") {
        fatal("log-message")
    }

    onSetup("start-fsm") {
        fsm.start()
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
            InitialCommandHandler,
            OnlineOfflineHandlers,
            OperationsAndDiagModeHandlers
    )
}
