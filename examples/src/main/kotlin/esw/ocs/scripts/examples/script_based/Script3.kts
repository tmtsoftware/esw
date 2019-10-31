//package esw.ocs.scripts.examples.script_based
//
//import csw.params.commands.CommandResponse.Completed
//import esw.ocs.dsl.core.script
//import esw.ocs.scripts.examples.reusable_scripts.script6
//import esw.ocs.scripts.examples.reusable_scripts.script7
//
//script {
//
//    val eventKey = "csw.a.b."
//
//    loadScripts(
//        script6,
//        script7
//    )
//
//    handleSetup("command-3") { command ->
//        log("============ command-3 ================")
//
//        val keys = (1..50).map { eventKey + it }.toTypedArray()
//
//        onEvent(*keys) { event ->
//            println("=======================")
//            log("Received: ${event.eventName()}")
//        }
//
//        log("============ command-3 End ================")
//        addOrUpdateCommand(Completed(command.runId()))
//    }
//
//    handleShutdown {
//        close()
//    }
//}
