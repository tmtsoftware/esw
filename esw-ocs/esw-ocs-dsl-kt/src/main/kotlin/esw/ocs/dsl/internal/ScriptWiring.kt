/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.internal

import esw.ocs.dsl.lowlevel.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import java.time.Duration

class ScriptWiring(val scriptContext: ScriptContext) {
    val heartbeatInterval: Duration by lazy { scriptContext.heartbeatInterval() }
    val strandEc: StrandEc by lazy { StrandEc.apply() }
    private val supervisorJob by lazy { SupervisorJob() }
    private val dispatcher by lazy { strandEc.executorService().asCoroutineDispatcher() }
    val scope: CoroutineScope by lazy { CoroutineScope(supervisorJob + dispatcher) }
    val cswServices: CswServices by lazy { CswServices.create(scriptContext, strandEc) }
    val heartbeatChannel: Channel<Unit> by lazy { Channel() }

    fun shutdown() {
        supervisorJob.cancel()
        dispatcher.close()
        heartbeatChannel.cancel()
    }
}
