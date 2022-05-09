/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package esw.ocs.script

import esw.ocs.script.impl.IvyResolver
import esw.ocs.script.impl.resolveFromAnnotations
import kotlinx.coroutines.runBlocking
import scala.util.Either
import scala.util.Left
import scala.util.Right
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

// Based on simple-main-kts from the kotlin-script-examples repo

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

const val COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR = "KOTLIN_ESW_KTS_COMPILED_SCRIPTS_CACHE_DIR"
const val COMPILED_SCRIPTS_CACHE_DIR_PROPERTY = "esw.sequence.kts.compiled.scripts.cache.dir"

class EswSequenceKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class)
        implicitReceivers(String::class)
        jvm {
//            val keyResource = EswSequenceKtsScriptDefinition::class.java.name.replace('.', '/') + ".class"
//            val thisJarFile =
//                EswSequenceKtsScriptDefinition::class.java.classLoader.getResource(keyResource)?.toContainingJarOrNull()
//            if (thisJarFile != null) {
//                dependenciesFromClassContext(
//                        EswSequenceKtsScriptDefinition::class,
//                        thisJarFile.name, "kotlin-stdlib", "kotlin-reflect", "kotlin-scripting-dependencies"
//                )
//            } else {
//            dependenciesFromClassContext(EswSequenceKtsScriptDefinition::class, wholeClasspath = true)
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
                val cacheExtSetting = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
                    ?: System.getenv(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR)
                val cacheBaseDir = when {
                    cacheExtSetting == null -> System.getProperty("java.io.tmpdir")
                        ?.let(::File)?.takeIf { it.exists() && it.isDirectory }
                        ?.let { File(it, "main.kts.compiled.cache").apply { mkdir() } }
                    cacheExtSetting.isBlank() -> null
                    else -> File(cacheExtSetting)
                }?.takeIf { it.exists() && it.isDirectory }
                if (cacheBaseDir != null)
                    compilationCache(
                        CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                            File(
                                cacheBaseDir,
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

//internal fun URL.toContainingJarOrNull(): File? =
//    if (protocol == "jar") {
//        (openConnection() as? JarURLConnection)?.jarFileURL?.toFileOrNull()
//    } else null

//internal fun URL.toFileOrNull() =
//    try {
//        File(toURI())
//    } catch (e: IllegalArgumentException) {
//        null
//    } catch (e: java.net.URISyntaxException) {
//        null
//    } ?: run {
//        if (protocol != "file") null
//        else File(file)
//    }

private fun <T> withEswKtsCacheDir(value: String?, body: () -> T): T {
    val prevCacheDir = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
    if (value == null) System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
    else System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, value)
    try {
        return body()
    } finally {
        if (prevCacheDir == null) System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
        else System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, prevCacheDir)
    }
}

private fun evalFile(scriptFile: File, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> {
    return withEswKtsCacheDir(cacheDir?.absolutePath ?: "") {
        val scriptDefinition = createJvmCompilationConfigurationFromTemplate<SequencerScript>()

        val evaluationEnv = EswKtsEvaluationConfiguration.with {
            jvm {
                baseClassLoader(null)
            }
            constructorArgs(emptyArray<String>())
            enableScriptsInstancesSharing()
        }

        BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), scriptDefinition, evaluationEnv)
    }
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
        println("XXX loadScript: returnValue = $x")
        return if (x is ResultValue.Value) {
            Right(x.value)
        } else {
            Left(errors.joinToString())
        }
    } catch (ex: Exception) {
        println("ScriptDef/loadScript() failed: $ex")
        ex.printStackTrace()
        return Left(ex.message)
    }
}

