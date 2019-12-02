package esw.ocs.dsl.highlevel

import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import kotlin.time.seconds

interface AlarmServiceDsl : LoopDsl {
    val alarmService: IAlarmService

    companion object {
        private val map: HashMap<AlarmKey, AlarmSeverity> = HashMap()
    }

    fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) {
        println(map)
        if (map.size == 0) startSetSeverity()
        map[alarmKey] = severity
    }

    private fun startSetSeverity() = bgLoop(5.seconds) {
        map.keys.forEach { key -> alarmService.setSeverity(key, map[key]) }
    }
}
