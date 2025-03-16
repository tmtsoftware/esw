package esw.ocs.dsl.highlevel

import org.apache.pekko.Done.done
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.models.Major
import esw.ocs.dsl.highlevel.models.TCS
import io.kotest.assertions.nondeterministic.eventually
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO after kotlin 1.5.x upgrade, class AlarmServiceDslTest : AlarmServiceDsl, LoopDsl does not work.
//  Somehow, LoopDsl needs to be added before AlarmServiceDsl.
class AlarmServiceDslTest : LoopDsl, AlarmServiceDsl {

    override val alarmService: IAlarmService = mockk()
    override val _alarmRefreshDuration: Duration = 1.seconds
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Suppress("DANGEROUS_CHARACTERS")
    @Test
    fun `AlarmServiceDsl_should_set_severity_of_alarms_and_refresh_it_|_ESW-125`() = runBlocking {
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

        eventually(5.seconds) {
            verify { alarmService.setSeverity(alarmKey1, severity) }
            verify { alarmService.setSeverity(alarmKey2, severity) }
            verify { alarmService.setSeverity(alarmKey3, severity) }
        }
    }
}
