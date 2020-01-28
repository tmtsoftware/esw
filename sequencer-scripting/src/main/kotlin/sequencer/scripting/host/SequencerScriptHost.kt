package sequencer.scripting.host

import sequencer.scripting.SequencerScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class SequencerScriptHost {

    private val scriptingHost = BasicJvmScriptingHost()

    fun eval(sourceCode: SourceCode): ResultWithDiagnostics<EvaluationResult> =
            scriptingHost.evalWithTemplate<SequencerScript>(
                    sourceCode,
                    evaluation = { constructorArgs() }
            )

    fun eval(sourceCode: String) =
            eval(sourceCode.toScriptSource())
}