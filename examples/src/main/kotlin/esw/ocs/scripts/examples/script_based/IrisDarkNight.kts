package esw.ocs.scripts.examples.script_based

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.script
import kotlinx.coroutines.future.await
import kotlin.time.seconds

script { _ ->

    val publishStream = publishEvent(10.seconds) {
        SystemEvent("iris.test", "system")
    }

    val subscriptionStream = onEvent("iris.test.system") {
        log("------------------> received-event for iris on key: ${it.eventKey()}")
    }

    onSetup("setup-iris") { command ->
        log("[Iris] Received command: ${command.commandName()}")

        val command1 = setup("esw.test-commandA1", "commandA1", "test-obsId")
        val command2 = setup("esw.test-commandA2", "commandA2", "test-obsId")

        val maybeCommandB = nextIf { it.commandName().name() == "setup-iris" }

        val assembly = Assembly("Sample1Assembly")

        maybeCommandB?.let {
            val commandB1 = setup("esw.test-commandB1", "setup-iris", "test-obsId")
            val commandB2 = setup("esw.test-commandB2", "setup-iris", "test-obsId")

            val assemblyResponse3 = assembly.submit(commandB1)
            if (assemblyResponse3 is CommandResponse.Error) finishWithError(assemblyResponse3.message())

            val assemblyResponse4 = assembly.submit(commandB2)
            if (assemblyResponse4 is CommandResponse.Error) finishWithError(assemblyResponse4.message())
        }

        val assemblyResponse1 = assembly.submit(command1)
        if (assemblyResponse1 is CommandResponse.Error) finishWithError(assemblyResponse1.message())

        val assemblyResponse2 = assembly.submit(command2)
        if (assemblyResponse2 is CommandResponse.Error) finishWithError(assemblyResponse2.message())
    }

    onShutdown {
        publishStream.cancel()
        subscriptionStream.unsubscribe().await()
        log("shutdown iris")
    }
}
