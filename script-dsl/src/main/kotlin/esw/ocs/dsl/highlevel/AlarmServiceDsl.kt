package esw.ocs.dsl.highlevel

import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.ocs.dsl.utils.bgLoop
import kotlin.time.seconds
import kotlinx.coroutines.CoroutineScope

interface AlarmServiceDsl : CoroutineScope {

    val alarmService: IAlarmService
    val alarmSeverityData: AlarmSeverityData

    private fun startSetSeverity() = bgLoop(5.seconds) {
        alarmSeverityData.map.keys.forEach { key -> alarmService.setSeverity(key, alarmSeverityData.map[key]) }
    }

    fun setSeverity(alarmKey: AlarmKey, _severity: AlarmSeverity) {
        if (alarmSeverityData.map.size == 0) startSetSeverity()
        alarmSeverityData.map[alarmKey] = _severity
    }
}
