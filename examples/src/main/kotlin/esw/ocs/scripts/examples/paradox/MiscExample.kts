@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.dsl.core.script
import esw.ocs.dsl.par
import kotlin.time.minutes

script {

    //#par
    val prefix = "ocs.primary"
    val hcd1 = Hcd("iris.filter.wheel1", 10.minutes)
    val hcd2 = Hcd("iris.filter.wheel2", 10.minutes)
    val hcd3 = Hcd("iris.filter.wheel3", 10.minutes)

    onSetup("setup-iris-hcds") {
        // send 3 setup commands to 3 HCD's in parallel
        val responses: List<SubmitResponse> =
                par(
                        { hcd1.submit(Setup(prefix, "move-10")) },
                        { hcd2.submit(Setup(prefix, "move-10")) },
                        { hcd3.submit(Setup(prefix, "move-10")) }
                )
    }
    //#par

}