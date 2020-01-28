package sequencer.scripting

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
        displayName = "Sequencer script",
        fileExtension = "sequencer.kts",
        compilationConfiguration = SequencerScriptCompilationConfiguration::class
)
abstract class SequencerScript

internal
object SequencerScriptCompilationConfiguration : ScriptCompilationConfiguration({
    baseClass(SequencerScript::class)

    jvm {
        dependenciesFromClassContext(
                SequencerScript::class,
                wholeClasspath = true
        )

    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }

    defaultImports(
            "esw.ocs.dsl.core.*",
            "esw.ocs.dsl.highlevel.models.*",
            "esw.ocs.dsl.*",
            "esw.ocs.dsl.params.*",
            "kotlin.time.*",
            "csw.params.commands.*",
            "csw.params.events.*",
            "csw.params.javadsl.JUnits.*",
            "csw.prefix.models.*"
    )
})