package esw.ocs.dsl.highlevel

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey

interface AlarmServiceDsl {
    fun setSeverity(alarmKey: AlarmKey, _severity: AlarmSeverity)
}