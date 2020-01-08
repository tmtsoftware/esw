package esw.ocs.dsl.highlevel

import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import io.kotlintest.matchers.beGreaterThanOrEqualTo
import io.kotlintest.matchers.beLessThanOrEqualTo
import io.kotlintest.should
import io.kotlintest.shouldBe
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
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration

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
    fun `TimeServiceDsl should scheduleOnce should delegate to timeServiceScheduler#scheduleOnce | ESW-122`() = runBlocking{
        every { scheduler.scheduleOnce(startTime, any<Runnable>()) }.answers { cancellable }
        scheduleOnce(startTime, mockk()) shouldBe cancellable
        verify { scheduler.scheduleOnce(startTime, any<Runnable>()) }
    }

    @Test
    fun `TimeServiceDsl should scheduleOnceFromNow should delegate to timeServiceScheduler#scheduleOnce | ESW-122`() = runBlocking{
        every { scheduler.scheduleOnce(any(), any<Runnable>()) }.answers { cancellable }
        scheduleOnceFromNow(1.seconds, mockk()) shouldBe cancellable
        verify { scheduler.scheduleOnce(any(), any<Runnable>()) }
    }

    @Test
    fun `schedulePeriodically should delegate to timeServiceScheduler#schedulePeriodically | ESW-122`()= runBlocking{
        every {
            scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>())
        }.answers { cancellable }

        schedulePeriodically(startTime, duration, mockk()) shouldBe cancellable
        verify { scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>()) }
    }

    @Test
    fun `schedulePeriodicallyFromNow should delegate to timeServiceScheduler#schedulePeriodically | ESW-122`()= runBlocking{
        every {
            scheduler.schedulePeriodically(any(), jDuration, any<Runnable>())
        }.answers { cancellable }

        schedulePeriodicallyFromNow(1.seconds, duration, mockk()) shouldBe cancellable
        verify { scheduler.schedulePeriodically(any(), jDuration, any<Runnable>()) }
    }

    @Test
    fun `offsetFromNow should give offset between current time and provided instance of the time | ESW-122`() {
        val offset: Duration = UTCTime.after(FiniteDuration(1, TimeUnit.SECONDS)).offsetFromNow()
        assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
    }

    @Test
    fun `utcTimeAfter should give UTC time of after given duration from now | ESW-122`() {
        val offset: Duration = utcTimeAfter(1.seconds).offsetFromNow()
        assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
    }

    @Test
    fun `taiTimeAfter should give time TAI of after given duration from now | ESW-122`() {
        val offset: Duration = taiTimeAfter(1.seconds).offsetFromNow()
        assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
    }
}
