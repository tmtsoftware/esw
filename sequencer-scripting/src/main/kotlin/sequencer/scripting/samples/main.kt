package sequencer.scripting.samples

import sequencer.scripting.host.SequencerScriptHost
import kotlin.script.experimental.api.valueOrThrow

fun main() {
    val host = SequencerScriptHost()
    val result = host.eval(
        """
            import csw.alarm.models.Key.AlarmKey
            val tromboneTemperatureAlarm =
                AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneMotorTemperatureAlarm")
            println(tromboneTemperatureAlarm)
        """.trimIndent()
    )
    result.valueOrThrow()
}