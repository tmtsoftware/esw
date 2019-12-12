package esw.ocs.dsl

import csw.params.commands.CommandResponse.*
import csw.params.commands.Result
import esw.ocs.dsl.highlevel.OtherError
import esw.ocs.dsl.highlevel.ScriptError
import esw.ocs.dsl.highlevel.SubmitError
import java.util.*

// =========== SubmitResponse ===========
val SubmitResponse.isStarted: Boolean get() = this is Started
val SubmitResponse.isSuccess: Boolean get() = this is Completed
val SubmitResponse.isFailed: Boolean get() = isNegative(this)

// =========== SubmitResponse - unsafe extensions ===========
val SubmitResponse.completed: SubmitResponse
    get() = if (this is Completed) this else throw SubmitError(this)

val SubmitResponse.result: Result
    get() = if (this is Completed) this.result() else throw SubmitError(this)

suspend fun SubmitResponse.onFailed(block: suspend () -> Unit): SubmitResponse {
    if (this.isFailed) block()
    return this
}

suspend fun SubmitResponse.onStarted(block: suspend () -> Unit): SubmitResponse {
    if (this.isStarted) block()
    return this
}

suspend fun SubmitResponse.onCompleted(block: suspend () -> Unit): SubmitResponse {
    if (this.isSuccess) block()
    return this
}


fun <T> Optional<T>.nullable(): T? = orElse(null)

internal fun Throwable.toScriptError(): ScriptError = when (this) {
    is SubmitError -> this
    else -> OtherError(this.message ?: "Unknown error", this)
}
