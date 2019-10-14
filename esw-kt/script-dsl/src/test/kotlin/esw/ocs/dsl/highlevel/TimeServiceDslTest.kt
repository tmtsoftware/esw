package esw.ocs.dsl.highlevel

import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import io.kotlintest.matchers.beGreaterThanOrEqualTo
import io.kotlintest.matchers.beLessThanOrEqualTo
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.*
import kotlinx.coroutines.CoroutineScope
import scala.concurrent.duration.FiniteDuration

class TimeServiceDslTest : WordSpec(), TimeServiceDsl {
    private val scheduler = mockk<TimeServiceScheduler>()
    private val cancellable = mockk<Cancellable>()

    private val startTime: UTCTime = UTCTime.now()
    private val duration: Duration = 10.seconds
    private val jDuration = duration.toJavaDuration()

    override val timeServiceScheduler: TimeServiceScheduler = scheduler
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private fun assertWithin(expected: Duration, result: Duration, tolerance: Duration) {
        val diff = expected - result
        diff should beLessThanOrEqualTo(tolerance)
        diff should beGreaterThanOrEqualTo(-tolerance)
    }

    init {
        "TimeServiceDsl" should {
            "scheduleOnce should delegate to timeServiceScheduler.scheduleOnce | ESW-122" {
                every { scheduler.scheduleOnce(startTime, any<Runnable>()) }.answers { cancellable }
                scheduleOnce(startTime, mockk()) shouldBe cancellable
                verify { scheduler.scheduleOnce(startTime, any<Runnable>()) }
            }

            "schedulePeriodically should delegate to timeServiceScheduler.schedulePeriodically | ESW-122" {
                every {
                    scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>())
                }.answers { cancellable }

                schedulePeriodically(startTime, duration, mockk()) shouldBe cancellable
                verify { scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>()) }
            }

            "offsetFromNow should give offset between current time and provided instance of the time" {
                val offset: Duration = UTCTime.after(FiniteDuration(1, TimeUnit.SECONDS)).offsetFromNow()
                assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
            }

            "utcTimeAfter should give UTC time of after given duration from now" {
                val offset: Duration = utcTimeAfter(1.seconds).offsetFromNow()
                assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
            }

            "taiTimeAfter should give time TAI of after given duration from now" {
                val offset: Duration = taiTimeAfter(1.seconds).offsetFromNow()
                assertWithin(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
            }
        }
    }
}
