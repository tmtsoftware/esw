package esw.ocs.dsl.internal

import esw.ocs.dsl.highlevel.OtherError
import esw.ocs.dsl.highlevel.ScriptError
import esw.ocs.dsl.highlevel.SubmitError

internal fun Throwable.toScriptError(): ScriptError = when (this) {
    is SubmitError -> this
    else -> OtherError(this.message ?: "Unknown error", this)
}
