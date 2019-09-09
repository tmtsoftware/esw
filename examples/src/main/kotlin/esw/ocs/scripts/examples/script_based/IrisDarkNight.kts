package esw.ocs.scripts.examples.script_based

import esw.ocs.dsl.core.script
import kotlinx.coroutines.future.await
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@UseExperimental(ExperimentalTime::class)
script { csw ->

    val publishStream = publishEvent(10.seconds) {
        systemEvent("iris.test", "system")
    }

    val subscriptionStream = onEvent("iris.test.system") {
        log("------------------> received-event for iris on key: ${it.eventKey()}")
    }

    handleSetup("setup-iris") { command ->
        log("[Iris] Received command: ${command.commandName()}")

        val command1 = setup("esw.test-commandA1", "commandA1", "test-obsId")
        val command2 = setup("esw.test-commandA2", "commandA2", "test-obsId")

        addSubCommand(command, command1)
        addSubCommand(command, command2)

        val maybeCommandB = nextIf { it.commandName().name() == "setup-iris" }

        maybeCommandB?.let { commandB ->
            val commandB1 = setup("esw.test-commandB1", "setup-iris", "test-obsId")
            val commandB2 = setup("esw.test-commandB2", "setup-iris", "test-obsId")

            addSubCommand(commandB, commandB1)
            addSubCommand(commandB, commandB2)

            /* ============= not yet supported in esw dsl ====================
            val assemblyResponse3 = csw.submit("Sample1Assembly", commandB1).await
            csw.updateSubCommand(subCmdResponse = assemblyResponse3)

            val assemblyResponse4 = csw.submit("Sample1Assembly", commandB2).await
            csw.updateSubCommand(subCmdResponse = assemblyResponse4)
            */

        }

        /* ============= not yet supported in esw dsl ====================
        val assemblyResponse1 = csw.submit("Sample1Assembly", command1).await
        csw.updateSubCommand(subCmdResponse = assemblyResponse1)

        val assemblyResponse2 = csw.submit("Sample1Assembly", command2).await
        csw.updateSubCommand(subCmdResponse = assemblyResponse2)
        */
    }

    handleShutdown {
        publishStream.cancel()
        subscriptionStream.unsubscribe().await()
        log("shutdown iris")
    }

}