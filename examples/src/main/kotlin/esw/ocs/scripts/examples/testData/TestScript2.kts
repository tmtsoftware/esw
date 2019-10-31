//package esw.ocs.scripts.examples.testData
//
//import csw.params.commands.CommandResponse.Completed
//import csw.params.commands.CommandResponse.Error
//import esw.ocs.dsl.core.reusableScript
//import esw.ocs.dsl.core.script
//import esw.ocs.dsl.params.set
//import kotlinx.coroutines.delay
//
//// ESW-134: Reuse code by ability to import logic from one script into another
//val onlineOfflineHandlers = reusableScript {
//    handleGoOffline {
//        goOfflineModeForSequencer("testSequencerId6", "testObservingMode6")
//        delay(1000)
//    }
//
//    handleGoOnline {
//        goOnlineModeForSequencer("testSequencerId6", "testObservingMode6")
//        delay(1000)
//    }
//}
//
//// ESW-134: Reuse code by ability to import logic from one script into another
//val operationsAndDiagModeHandlers = reusableScript {
//    handleDiagnosticMode { startTime, hint ->
//        // do some actions to go to diagnostic mode based on hint
//        diagnosticModeForSequencer(
//            "testSequencerId6", "testObservingMode6",
//            startTime,
//            hint
//        )
//    }
//
//    handleOperationsMode {
//        // do some actions to go to operations mode
//        operationsModeForSequencer("testSequencerId6", "testObservingMode6")
//    }
//}
//
//script {
//    handleSetup("command-1") { command ->
//
//        // To avoid sequencer to finish immediately so that other Add, Append command gets time
//        delay(200)
//        addOrUpdateCommand(Completed(command.runId))
//    }
//
//    handleSetup("command-2") { command ->
//
//        addOrUpdateCommand(Completed(command.runId))
//    }
//
//    handleSetup("command-3") { command ->
//
//        addOrUpdateCommand(Completed(command.runId))
//    }
//
//    handleSetup("command-4") { command ->
//        //Don't complete immediately as this is used to abort sequence usecase
//        delay(700)
//        addOrUpdateCommand(Completed(command.runId))
//    }
//
//    handleSetup("command-5") { command ->
//
//        addOrUpdateCommand(Completed(command.runId))
//    }
//
//    handleSetup("command-6") { command ->
//
//        addOrUpdateCommand(Completed(command.runId))
//    }
//
//    handleSetup("fail-command") { command ->
//
//        addOrUpdateCommand(Error(command.runId, command.commandName().name()))
//    }
//
//    // ESW-134: Reuse code by ability to import logic from one script into another
//    loadScripts(
//        onlineOfflineHandlers,
//        operationsAndDiagModeHandlers
//    )
//}
