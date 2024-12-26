package esw.ocs.dsl.highlevel

import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import io.kotest.matchers.comparables.beGreaterThanOrEqualTo
import io.kotest.matchers.comparables.beLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Suppress("DANGEROUS_CHARACTERS")
class TimeServiceDslTest : TimeServiceDsl {
    private val scheduler = mockk<TimeServiceScheduler>()
    private val cancellable = mockk<Cancellable>()

    private val startTime: UTCTime = utcTimeNow()
    private val duration: Duration = 10.seconds
    private val jDuration = duration.toJavaDuration()

    override val timeService: TimeServiceScheduler = scheduler
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private fun assertWithin(expected: Duration, result: Duration, tolerance: Duration) {
        val diff = expected - result
        diff should beLessThanOrEqualTo(tolerance)
        diff should beGreaterThanOrEqualTo(-tolerance)
    }

    @Test
    fun `TimeServiceDsl_should_scheduleOnce_should_delegate_to_timeServiceScheduler#scheduleOnce_|_ESW-122`() = runBlocking {
        every { scheduler.scheduleOnce(startTime, any<Runnable>()) }.answers { cancellable }
        scheduleOnce(startTime, mockk()) shouldBe cancellable
        verify { scheduler.scheduleOnce(startTime, any<Runnable>()) }
    }

    @Test
    fun `TimeServiceDsl_should_scheduleOnceFromNow_should_delegate_to_timeServiceScheduler#scheduleOnce_|_ESW-122`() = runBlocking {
        every { scheduler.scheduleOnce(any(), any<Runnable>()) }.answers { cancellable }
        scheduleOnceFromNow(1.seconds, mockk()) shouldBe cancellable
        verify { scheduler.scheduleOnce(any(), any<Runnable>()) }
    }

    @Test
    fun `schedulePeriodically_should_delegate_to_timeServiceScheduler#schedulePeriodically_|_ESW-122`() = runBlocking {
        every {
            scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>())
        }.answers { cancellable }

        schedulePeriodically(startTime, duration, mockk()) shouldBe cancellable
        verify { scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>()) }
    }

    @Test
    fun `schedulePeriodicallyFromNow_should_delegate_to_timeServiceScheduler#schedulePeriodically_|_ESW-122`() = runBlocking {
        every {
            scheduler.schedulePeriodically(any(), jDuration, any<Runnable>())
        }.answers { cancellable }

        schedulePeriodicallyFromNow(1.seconds, duration, mockk()) shouldBe cancellable
        verify { scheduler.schedulePeriodically(any(), jDuration, any<Runnable>()) }
    }

    @Test
    fun `offsetFromNow_should_give_offset_between_current_time_and_provided_instance_of_the_time_|_ESW-122`() {
        val offset: Duration = UTCTime.after(FiniteDuration(1, TimeUnit.SECONDS)).offsetFromNow()
        assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
    }

    @Test
    fun `utcTimeAfter_should_give_UTC_time_of_after_given_duration_from_now_|_ESW-122`() {
        val offset: Duration = utcTimeAfter(1.seconds).offsetFromNow()
        assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
    }

    @Test
    fun `taiTimeAfter_should_give_time_TAI_of_after_given_duration_from_now_|_ESW-122`() {
        val offset: Duration = taiTimeAfter(1.seconds).offsetFromNow()
        assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
    }
}
