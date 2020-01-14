package esw.ocs.dsl.highlevel

import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlin.time.Duration

interface AlarmServiceDsl : LoopDsl {
    val alarmService: IAlarmService
    val _alarmRefreshDuration: Duration

    companion object {
        private val keyToSeverityMap: HashMap<AlarmKey, AlarmSeverity> = HashMap()
    }

    /**
     * Sets alarm severity against provided alarm key and keeps refreshing it after every `csw-alarm.refresh-interval` which by default is 3 seconds
     *
     * @param alarmKey unique alarm in alarm store e.g nfiraos.trombone.tromboneaxislowlimitalarm
     * @param severity severity to be set for the alarm e.g. Okay, Warning, Major, Critical, etc
     */
    suspend fun setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity) {
        keyToSeverityMap[alarmKey] = severity
        alarmService.setSeverity(alarmKey, severity).await()
        if (keyToSeverityMap.size == 1)
            delayTaskExecution(_alarmRefreshDuration) { startSetSeverity() } // alarm is already set at this stage, hence wait for refresh interval and then start loop
    }

    private fun delayTaskExecution(delayDuration: Duration, task: () -> Unit) = coroutineScope.launch {
        delay(delayDuration.toLongMilliseconds())
        task()
    }

    private fun startSetSeverity() = loopAsync(_alarmRefreshDuration) {
        keyToSeverityMap.map { (key, severity) -> alarmService.setSeverity(key, severity).asDeferred() }
    }
}
