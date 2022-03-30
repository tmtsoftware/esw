/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel.models

import csw.params.commands.CommandResponse

sealed class ScriptError(cause: Throwable? = null) : Exception(cause) {
    abstract val reason: String
    override val message: String? get() = reason
}

data class CommandError(val submitResponse: CommandResponse.SubmitResponse) : ScriptError() {
    override val reason: String = submitResponse.toString()
}

data class OtherError(override val reason: String, override val cause: Throwable? = null) : ScriptError(cause)
