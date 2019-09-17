package esw.ocs.scripts.examples.script_based

import esw.ocs.dsl.core.script
import kotlin.time.seconds
import kotlinx.coroutines.future.await

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

            val assemblyResponse3 = submitCommandToAssembly("Sample1Assembly", commandB1)
            updateSubCommand(assemblyResponse3)

            val assemblyResponse4 = submitCommandToAssembly("Sample1Assembly", commandB2)
            updateSubCommand(assemblyResponse4)
        }

        val assemblyResponse1 = submitCommandToAssembly("Sample1Assembly", command1)
        updateSubCommand(assemblyResponse1)

        val assemblyResponse2 = submitCommandToAssembly("Sample1Assembly", command2)
        updateSubCommand(assemblyResponse2)
    }

    handleShutdown {
        publishStream.cancel()
        subscriptionStream.unsubscribe().await()
        log("shutdown iris")
    }
}
