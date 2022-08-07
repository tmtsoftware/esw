/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package esw.ocs.script

import esw.ocs.script.impl.IvyResolver
import esw.ocs.script.impl.resolveFromAnnotations
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

import scala.util.Either
import scala.util.Left
import scala.util.Right

@Suppress("unused")
// The KotlinScript annotation marks a class that can serve as a reference to the script definition for
// `createJvmCompilationConfigurationFromTemplate` call as well as for the discovery mechanism
// The marked class also become the base class for defined script type (unless redefined in the configuration)
@KotlinScript(
    // file name extension by which this script type is recognized by mechanisms built into scripting compiler plugin
    // and IDE support, it is recommended to use double extension with the last one being "kts", so some non-specific
    // scripting support could be used, e.g. in IDE, if the specific support is not installed.
    fileExtension = "seq.kts",
    // the class or object that defines script compilation configuration for this type of scripts
    compilationConfiguration = EswSequenceKtsScriptDefinition::class,
    // the class or object that defines script evaluation configuration for this type of scripts
    evaluationConfiguration = EswKtsEvaluationConfiguration::class
)
// the class is used as the script base class, therefore it should be open or abstract. Also the constructor parameters
// of the base class are copied to the script constructor, so with this definition the script will require `args` to be
// passed to the constructor, and `args` could be used in the script as a defined variable.
abstract class SequencerScript(val args: Array<String>)

class EswSequenceKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        val cacheDir = File("${System.getProperty("user.home")}/.esw-script-cache")
        if (!cacheDir.isDirectory) cacheDir.mkdirs()

        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class)
        implicitReceivers(String::class)
        jvm {
//            val keyResource = EswSequenceKtsScriptDefinition::class.java.name.replace('.', '/') + ".class"
//            val thisJarFile = EswSequenceKtsScriptDefinition::class.java.classLoader.getResource(keyResource)?.toContainingJarOrNull()
//            if (thisJarFile != null) {
//                dependenciesFromClassContext(
//                        EswSequenceKtsScriptDefinition::class,
//                        thisJarFile.name, "kotlin-stdlib", "kotlin-reflect", "kotlin-scripting-dependencies"
//                )
//            } else {
//                dependenciesFromClassContext(EswSequenceKtsScriptDefinition::class, wholeClasspath = true)
//            }
            dependenciesFromCurrentContext(wholeClasspath = true)
        }

        refineConfiguration {
            onAnnotations(
                DependsOn::class,
                Repository::class,
                Import::class,
                CompilerOptions::class,
                handler = MainKtsConfigurator()
            )
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        hostConfiguration(ScriptingHostConfiguration {
            jvm {
                compilationCache(
                    CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                        File(
                            cacheDir,
                            compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar"
                        )
                    }
                )
            }
        })
    })

object EswKtsEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        implicitReceivers("")
        refineConfigurationBeforeEvaluate(::configureConstructorArgsFromMainArgs)
    }
)

fun configureConstructorArgsFromMainArgs(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val mainArgs = context.evaluationConfiguration[ScriptEvaluationConfiguration.jvm.mainArguments]
    val res =
        if (context.evaluationConfiguration[ScriptEvaluationConfiguration.constructorArgs] == null && mainArgs != null) {
            context.evaluationConfiguration.with {
                constructorArgs(mainArgs)
            }
        } else context.evaluationConfiguration
    return res.asSuccess()
}

class MainKtsConfigurator : RefineScriptCompilationConfigurationHandler {
    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), IvyResolver())

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        processAnnotations(context)

    private fun processAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources = annotations.flatMap {
            (it as? Import)?.paths?.map { sourceName ->
                FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
            } ?: emptyList()
        }
        val compileOptions = annotations.flatMap {
            (it as? CompilerOptions)?.options?.toList() ?: emptyList()
        }

        val resolveResult = try {
            runBlocking {
                resolveFromAnnotations(resolver, annotations.filter { it is DependsOn || it is Repository })
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(
                *diagnostics.toTypedArray(),
                e.asDiagnostics(path = context.script.locationId)
            )
        }

        return resolveResult.onSuccess { resolvedClassPath ->
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                updateClasspath(resolvedClassPath)
                if (importedSources.isNotEmpty()) importScripts.append(importedSources)
                if (compileOptions.isNotEmpty()) compilerOptions.append(compileOptions)
            }.asSuccess()
        }
    }
}

private fun compiledScriptUniqueName(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    val scriptDefinition = createJvmCompilationConfigurationFromTemplate<SequencerScript>()

    val evaluationEnv = EswKtsEvaluationConfiguration.with {
        jvm {
//                baseClassLoader(null)
            baseClassLoader(Thread.currentThread().contextClassLoader)
        }
        constructorArgs(emptyArray<String>())
        enableScriptsInstancesSharing()
    }

    return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), scriptDefinition, evaluationEnv)
}

// Loads the given Kotlin script and returns a Scala result with either the error
// message (String) or the result (Any).
// In order to avoid a recursive dependency, the type Any instead of ScriptResult.
// The result can be cast in order to access the invoke() function to execute the script.
fun loadScript(scriptFile: File): Either<String, Any> {
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
    } else if (x is ResultValue.Error) {
        x.error.printStackTrace()
        throw x.error
    } else {
        Left(errors.joinToString())
    }
}

