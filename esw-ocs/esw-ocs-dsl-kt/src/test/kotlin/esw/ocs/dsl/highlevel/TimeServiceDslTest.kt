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
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration

class TimeServiceDslTest : TimeServiceDsl {
    private val scheduler = mockk<TimeServiceScheduler>()
    private val cancellable = mockk<Cancellable>()

    private val startTime: UTCTime = utcTimeNow()
    private val duration: Duration = Duration.seconds(10)
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
        scheduleOnceFromNow(Duration.seconds(1), mockk()) shouldBe cancellable
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

        schedulePeriodicallyFromNow(Duration.seconds(1), duration, mockk()) shouldBe cancellable
        verify { scheduler.schedulePeriodically(any(), jDuration, any<Runnable>()) }
    }

    @Test
    fun `offsetFromNow should give offset between current time and provided instance of the time | ESW-122`() {
        val offset: Duration = UTCTime.after(FiniteDuration(1, TimeUnit.SECONDS)).offsetFromNow()
        assertWithin(expected = Duration.seconds(1), result = offset, tolerance = Duration.milliseconds(2))
    }

    @Test
    fun `utcTimeAfter should give UTC time of after given duration from now | ESW-122`() {
        val offset: Duration = utcTimeAfter(Duration.seconds(1)).offsetFromNow()
        assertWithin(expected = Duration.seconds(1), result = offset, tolerance = Duration.milliseconds(2))
    }

    @Test
    fun `taiTimeAfter should give time TAI of after given duration from now | ESW-122`() {
        val offset: Duration = taiTimeAfter(Duration.seconds(1)).offsetFromNow()
        assertWithin(expected = Duration.seconds(1), result = offset, tolerance = Duration.milliseconds(2))
    }
}
