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

class AlarmServiceDslTest : WordSpec(), AlarmServiceDsl {

    private val mockedAlarmService: IAlarmService = mockk()

    override val coroutineContext: CoroutineContext = EmptyCoroutineContext
    override val alarmService: IAlarmService = mockedAlarmService
    override val alarmSeverityData: AlarmSeverityData = AlarmSeverityData(HashMap())

    init {
        "AlarmServiceDsl" should {
            "set " {

                val alarmKey = AlarmKey(TCS, "filter_assembly", "temperature")
                val severity = Major

                every {
                    mockedAlarmService.setSeverity(alarmKey, severity)
                } answers { completedFuture(done()) }

                setSeverity(alarmKey, severity)

                eventually(Duration.ofSeconds(5)) {
                    verify { mockedAlarmService.setSeverity(alarmKey, severity) }
                }
            }
        }
    }
}
