@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Setup
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.IRIS
import esw.ocs.dsl.par
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.minutes

script {

    //#par
    val prefix = "OCS.IRIS_darkMode"
    val hcd1 = Hcd(IRIS, "filter.wheel1", Duration.minutes(10))
    val hcd2 = Hcd(IRIS, "filter.wheel2", Duration.minutes(10))
    val hcd3 = Hcd(IRIS, "filter.wheel3", Duration.minutes(10))

    onSetup("setup-iris-hcds") {
        // send 3 setup commands to 3 HCD's in parallel
        val responses: List<SubmitResponse> =
            par(
                { hcd1.submitAndWait(Setup(prefix, "move-10")) },
                { hcd2.submitAndWait(Setup(prefix, "move-10")) },
                { hcd3.submitAndWait(Setup(prefix, "move-10")) }
            )
    }
    //#par

    //#isOnline
    onDiagnosticMode { _, _ ->
        loopAsync(Duration.milliseconds(100)) {
            if (isOnline)
                publishEvent(SystemEvent("TCS.filter.wheel", "online-diag-data"))
            else
                publishEvent(SystemEvent("TCS.filter.wheel", "offline-diag-data"))
        }

    }
    //#isOnline

    //#prefix
    val downstreamCommand: Setup = Setup(prefix, "move")
    //#prefix

}