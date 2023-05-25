package esw.ocs.dsl2.highlevel

import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.ocs.dsl.script.StrandEc

import scala.concurrent.duration.Duration
import async.Async.*
import scala.concurrent.{ExecutionContext, Future}

class AlarmServiceDsl(alarmService: AlarmService, _alarmRefreshDuration: Duration, loopDsl: LoopDsl)(using ExecutionContext):
  private var keyToSeverityMap: Map[AlarmKey, AlarmSeverity] = Map()

  inline def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Unit = await(setSeverityAsync(alarmKey, severity))

  private def setSeverityAsync(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    keyToSeverityMap += (alarmKey -> severity)
    await(alarmService.setSeverity(alarmKey, severity))
    if (keyToSeverityMap.size == 1)
      // alarm is already set at this stage, hence wait for refresh interval and then start loop
      await {
        delayTaskExecution(_alarmRefreshDuration) {
          startSetSeverity()
        }
      }
  }

  private def delayTaskExecution(delayDuration: Duration)(task: => Unit): Future[Unit] = async {
    loopDsl.delay(delayDuration)
    task
  }

  private def startSetSeverity() = loopDsl.loopAsync(_alarmRefreshDuration) {
    val futures = keyToSeverityMap.map { (key, severity) =>
      alarmService.setSeverity(key, severity)
    }
    await(Future.sequence(futures))
  }

end AlarmServiceDsl
