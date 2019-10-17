package esw.ocs.dsl.highlevel

import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.ocs.dsl.utils.bgLoop
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.seconds

interface AlarmServiceDsl {
    fun setSeverity(alarmKey: AlarmKey, _severity: AlarmSeverity)
}

class AlarmServiceDslImpl(private val alarmService: IAlarmService) : AlarmServiceDsl {
    private val map: HashMap<AlarmKey, AlarmSeverity> = HashMap()
    // fixme: Which scope should be used here?
    private val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private fun startSetSeverity() = scope.bgLoop(5.seconds) {
        map.keys.forEach { key -> alarmService.setSeverity(key, map[key]) }
    }

    override fun setSeverity(alarmKey: AlarmKey, _severity: AlarmSeverity) {
        if (map.size == 0) startSetSeverity()
        map[alarmKey] = _severity
    }
}