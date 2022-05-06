package esw.ocs.script

import scala.util.Either
import scala.util.Left
import scala.util.Right
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SequencerScript> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    }

    return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), compilationConfiguration, null)
}

// Loads the given Kotlin script and returns a Scala result with either the error
// message (String) or the result (Any).
// In order to avoid a recursive dependency, the type Any instead of ScriptResult.
// The result can be cast in order to access the invoke() function to execure the script.
fun loadScript(scriptFile: File): Either<String, Any> {
    try {
        val t0 = System.nanoTime()
        val res = evalFile(scriptFile)
        val t1 = System.nanoTime()
        val d = t1 - t0
        val ms = d / 1000000
        println("XXXXXXXXXXXXX Time to load $scriptFile: $d ns ($ms ms)")
        val errors = res.reports.map { it.message }
        val x = res.valueOrNull()?.returnValue
        return if (x is ResultValue.Value) {
            Right(x.value)
        } else {
            Left(errors.joinToString())
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
        return Left(ex.message)
    }
}

