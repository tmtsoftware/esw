package esw.ocs.scripts.examples.testData

import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay
import scala.jdk.javaapi.CollectionConverters
import java.util.*

script {
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
        val sequence = Sequence(
                Id("testSequenceIdString123"),
                CollectionConverters.asScala(Collections.singleton<SequenceCommand>(command)).toSeq()
        )

        val tcs = Sequencer("tcs", "moonnight")
        tcs.submitAndWait(sequence)
    }

    // ESW-134: Reuse code by ability to import logic from one script into another
    loadScripts(
        InitialCommandHandler,
        OnlineOfflineHandlers,
        OperationsAndDiagModeHandlers
    )
}
