package esw.ocs.dsl.highlevel

import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import kotlin.time.Duration

interface AlarmServiceDsl : LoopDsl {
    val alarmService: IAlarmService
    val _alarmRefreshDuration: Duration

    companion object {
        private val map: HashMap<AlarmKey, AlarmSeverity> = HashMap()
    }

    // sets the provided [AlarmSeverity] against provided [AlarmKey] and keeps refreshing the same every 5 seconds
    fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) {
        map += alarmKey to severity
        if (map.size == 1) startSetSeverity()
    }

    private fun startSetSeverity() = loopAsync(_alarmRefreshDuration) {
        map.keys.forEach { key -> alarmService.setSeverity(key, map[key]) }
    }
}
