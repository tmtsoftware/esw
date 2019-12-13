package esw.ocs.dsl.highlevel

import com.typesafe.config.ConfigFactory
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

interface AlarmServiceDsl : LoopDsl {
    val alarmService: IAlarmService

    companion object {
        private val map: HashMap<AlarmKey, AlarmSeverity> = HashMap()

        // If there is need to read config for some other place in dsl module, add functionality to pass it from SequencerWiring.
        private val alarmConfig by lazy { ConfigFactory.load().getConfig("csw-alarm") }
        private val alarmRefreshDuration: Duration by lazy { alarmConfig.getDuration("refresh-interval").toKotlinDuration() }
    }

    // sets the provided [AlarmSeverity] against provided [AlarmKey] and keeps refreshing the same every 5 seconds
    fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) {
        map += alarmKey to severity
        if (map.size == 1) startSetSeverity()
    }

    private fun startSetSeverity() = bgLoop(alarmRefreshDuration) {
        map.keys.forEach { key -> alarmService.setSeverity(key, map[key]) }
    }
}
