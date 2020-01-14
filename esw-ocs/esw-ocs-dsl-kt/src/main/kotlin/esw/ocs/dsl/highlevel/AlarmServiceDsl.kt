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

    /**
     * Sets alarm severity against provided alarm key and keeps refreshing it after every `csw-alarm.refresh-interval` which by default is 3 seconds
     *
     * @param alarmKey unique alarm in alarm store e.g nfiraos.trombone.tromboneaxislowlimitalarm
     * @param severity severity to be set for the alarm e.g. Okay, Warning, Major, Critical, etc
     *
     */
    fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) {
        map += alarmKey to severity
        if (map.size == 1) startSetSeverity()
    }

    private fun startSetSeverity() = loopAsync(_alarmRefreshDuration) {
        map.keys.forEach { key -> alarmService.setSeverity(key, map[key]) }
    }
}
