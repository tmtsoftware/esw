package esw.ocs.dsl.highlevel

import akka.Done.done
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.models.Major
import esw.ocs.dsl.highlevel.models.TCS
import io.kotest.assertions.timing.eventually
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.seconds

// TODO after kotlin 1.5.x upgrade, class AlarmServiceDslTest : AlarmServiceDsl, LoopDsl does not work.
//  Somehow, LoopDsl needs to be added before AlarmServiceDsl.
class AlarmServiceDslTest : LoopDsl, AlarmServiceDsl {

    override val alarmService: IAlarmService = mockk()
    override val _alarmRefreshDuration: Duration = Duration.seconds(3)
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Test
    fun `AlarmServiceDsl should set severity of alarms and refresh it | ESW-125`() = runBlocking {
        val alarmKey1 = AlarmKey(Prefix(TCS, "filter_assembly1"), "temperature1")
        val alarmKey2 = AlarmKey(Prefix(TCS, "filter_assembly2"), "temperature2")
        val alarmKey3 = AlarmKey(Prefix(TCS, "filter_assembly3"), "temperature3")

        val severity = Major
        val doneF = completedFuture(done())

        every { alarmService.setSeverity(alarmKey1, severity) } answers { doneF }
        every { alarmService.setSeverity(alarmKey2, severity) } answers { doneF }
        every { alarmService.setSeverity(alarmKey3, severity) } answers { doneF }

        setSeverity(alarmKey1, severity)
        setSeverity(alarmKey2, severity)
        setSeverity(alarmKey3, severity)

        verify { alarmService.setSeverity(alarmKey1, severity) }
        verify { alarmService.setSeverity(alarmKey2, severity) }
        verify { alarmService.setSeverity(alarmKey3, severity) }

        // to test on refresh functionality
        clearMocks(alarmService)
        every { alarmService.setSeverity(alarmKey1, severity) } answers { doneF }
        every { alarmService.setSeverity(alarmKey2, severity) } answers { doneF }
        every { alarmService.setSeverity(alarmKey3, severity) } answers { doneF }

        eventually(Duration.seconds(5)) {
            verify { alarmService.setSeverity(alarmKey1, severity) }
            verify { alarmService.setSeverity(alarmKey2, severity) }
            verify { alarmService.setSeverity(alarmKey3, severity) }
        }
    }
}
