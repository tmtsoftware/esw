package esw.ocs.dsl.highlevel

import akka.Done.done
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.javadsl.JAlarmSeverity.Major
import csw.alarm.models.Key.AlarmKey
import csw.params.javadsl.JSubsystem.TCS
import io.kotlintest.eventually
import io.kotlintest.seconds
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture.completedFuture
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlinx.coroutines.SupervisorJob

class AlarmServiceDslTest : AlarmServiceDsl {

    private val mockedAlarmService: IAlarmService = mockk()

    override val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob())
    override val alarmService: IAlarmService = mockedAlarmService
    override val alarmSeverityData: AlarmSeverityData = AlarmSeverityData(HashMap())

    @Test
    fun `AlarmServiceDsl should set severity of alarms | ESW-125`() {

        val alarmKey = AlarmKey(TCS, "filter_assembly", "temperature")
        val severity = Major

        every {
            mockedAlarmService.setSeverity(alarmKey, severity)
        } answers { completedFuture(done()) }

        setSeverity(alarmKey, severity)

        eventually(5.seconds) {
            verify { mockedAlarmService.setSeverity(alarmKey, severity) }
        }
    }
}
