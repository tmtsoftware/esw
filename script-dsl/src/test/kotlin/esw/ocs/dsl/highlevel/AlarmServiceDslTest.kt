package esw.ocs.dsl.highlevel

import akka.Done.done
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.javadsl.JAlarmSeverity.Major
import csw.alarm.models.Key.AlarmKey
import csw.params.javadsl.JSubsystem.TCS
import io.kotlintest.eventually
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class AlarmServiceDslTest : WordSpec({

    class Mocks {
        val _alarmService: IAlarmService = mockk()

        val alarmServiceDsl = object : AlarmServiceDsl {
            override val coroutineContext: CoroutineContext
                get() = EmptyCoroutineContext
            override val alarmService = _alarmService
            override val alarmSeverityData = AlarmSeverityData(HashMap())
        }
    }

    "AlarmServiceDsl" should {
        "set " {

            with(Mocks()) {
                val alarmKey = AlarmKey(TCS, "filter_assembly", "temperature")
                val severity = Major

                every {
                    _alarmService.setSeverity(alarmKey, severity)
                } answers { completedFuture(done()) }

                alarmServiceDsl.setSeverity(alarmKey, severity)

                eventually(Duration.ofSeconds(5)) {
                    verify { _alarmService.setSeverity(alarmKey, severity) }
                }
            }
        }
    }
})
