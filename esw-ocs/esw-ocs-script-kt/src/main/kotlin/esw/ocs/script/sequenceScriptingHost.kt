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

import kotlin.script.experimental.annotations.KotlinScript

// The KotlinScript annotation marks a class that can serve as a reference to the script definition for
// `createJvmCompilationConfigurationFromTemplate` call as well as for the discovery mechanism
// The marked class also become the base class for defined script type (unless redefined in the configuration)
@KotlinScript(
    // file name extension by which this script type is recognized by mechanisms built into scripting compiler plugin
    // and IDE support, it is recommended to use double extension with the last one being "kts", so some non-specific
    // scripting support could be used, e.g. in IDE, if the specific support is not installed.
    fileExtension = "seq.kts"
)
// the class is used as the script base class, therefore it should be open or abstract
abstract class SequencerScript

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SequencerScript> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    }

    return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), compilationConfiguration, null)
}

fun scalaEval(scriptFile: File): Either<String, Any> {
    println("XXX1 Executing script $scriptFile")
    val res = evalFile(scriptFile)
    println("XXX res.reports len = ${res.reports.size}")
    val errors = res.reports.map {
        if (it.severity > ScriptDiagnostic.Severity.DEBUG) {
            println("XXX1 msgX: ${it.message}")
            if (it.exception != null)
                it.exception!!.printStackTrace()
            val s = it.message + if (it.exception == null) "" else ": ${it.exception.toString()}"
            println("XXX1 msg: $s")
            s
        } else "XXX"
    }
    println("XXX1 scalaEval: res = $res")
    val x = res.valueOrNull()?.returnValue
    println("XXX1 scalaEval: x = $x")
    return if (x is ResultValue.Value) {
        println("XXX scalaEval: x.value = ${x.value}")
        Right(x.value)
    } else
        Left(errors.joinToString())
}

