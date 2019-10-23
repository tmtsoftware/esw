package esw.ocs.dsl.highlevel

import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import kotlinx.coroutines.CoroutineScope
import kotlin.time.seconds

interface AlarmServiceDsl {
    fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity)
}

class AlarmServiceDslImpl(private val alarmService: IAlarmService, override val coroutineScope: CoroutineScope) : AlarmServiceDsl, LoopDsl {
    private val map: HashMap<AlarmKey, AlarmSeverity> = HashMap()

    private fun startSetSeverity() = bgLoop(5.seconds) {
        map.keys.forEach { key -> alarmService.setSeverity(key, map[key]) }
    }

    override fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) {
        if (map.size == 0) startSetSeverity()
        map[alarmKey] = severity
    }
}