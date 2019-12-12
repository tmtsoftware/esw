package esw.ocs.dsl.highlevel.models

import csw.params.commands.CommandResponse

sealed class ScriptError(cause: Throwable? = null) : Exception(cause) {
    abstract val reason: String
    override val message: String? get() = reason
}

data class CommandError(val submitResponse: CommandResponse.SubmitResponse) : ScriptError() {
    override val reason: String =
            when (submitResponse) {
                is CommandResponse.Error -> submitResponse.message()
                is CommandResponse.Invalid -> submitResponse.issue().reason()
                else -> "Command id = ${submitResponse.runId()}, SubmitResponse = ${submitResponse.javaClass.simpleName}"
            }
}

data class OtherError(override val reason: String, override val cause: Throwable? = null) : ScriptError(cause)
