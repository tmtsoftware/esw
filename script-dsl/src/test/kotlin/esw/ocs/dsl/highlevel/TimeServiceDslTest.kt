package esw.ocs.dsl.highlevel

import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import io.kotlintest.matchers.beGreaterThanOrEqualTo
import io.kotlintest.matchers.beLessThanOrEqualTo
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.*
import kotlinx.coroutines.Job
import scala.concurrent.duration.FiniteDuration

class TimeServiceDslTest : WordSpec() {
    class Mocks {
        val scheduler = mockk<TimeServiceScheduler>()
        val cancellable = mockk<Cancellable>()

        val startTime: UTCTime = UTCTime.now()
        val duration: Duration = 10.seconds
        val jDuration = duration.toJavaDuration()
        val context = Job()

        val timeServiceDsl = object : TimeServiceDsl {
            override val timeServiceScheduler: TimeServiceScheduler = scheduler
            override val coroutineContext: CoroutineContext = context
        }
    }

    abstract class TestTimeServiceDsl(mocks: Mocks) : TimeServiceDsl {
        override val timeServiceScheduler: TimeServiceScheduler = mocks.timeServiceDsl.timeServiceScheduler
        override val coroutineContext: CoroutineContext = mocks.timeServiceDsl.coroutineContext
    }

    private fun checkWithIn(expected: Duration, result: Duration, tolerance: Duration) {
        val diff = expected - result
        diff shouldBe beLessThanOrEqualTo(tolerance)
        diff shouldBe beGreaterThanOrEqualTo(-tolerance)
    }

    init {
        "TimeServiceDsl" should {
            "scheduleOnce should delegate to timeServiceScheduler.scheduleOnce | ESW-122" {
                with(Mocks()) {
                    every { scheduler.scheduleOnce(startTime, any<Runnable>()) }.answers { cancellable }

                    timeServiceDsl.scheduleOnce(startTime, mockk()) shouldBe cancellable

                    verify { scheduler.scheduleOnce(startTime, any<Runnable>()) }
                }
            }

            "schedulePeriodically should delegate to timeServiceScheduler.schedulePeriodically | ESW-122" {
                with(Mocks()) {
                    every {
                        scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>())
                    }.answers { cancellable }

                    timeServiceDsl.schedulePeriodically(startTime, duration, mockk()) shouldBe cancellable

                    verify { scheduler.schedulePeriodically(startTime, jDuration, any<Runnable>()) }
                }
            }

            "offsetFromNow should give offset between current time and provided instance of the time" {
                with(Mocks()) {
                    val dsl = object : TestTimeServiceDsl(this) {
                        fun testOffset() {
                            val offset: Duration = UTCTime.after(FiniteDuration(1, TimeUnit.SECONDS)).offsetFromNow()
                            checkWithIn(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
                        }
                    }

                    dsl.testOffset()
                }
            }

            "utcTimeAfter should give UTC time of after given duration from now" {
                with(Mocks()) {
                    val dsl = object : TestTimeServiceDsl(this) {
                        fun testUtcAfter() {
                            val offset: Duration = utcTimeAfter(1.seconds).offsetFromNow()
                            checkWithIn(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
                        }
                    }

                    dsl.testUtcAfter()
                }
            }

            "taiTimeAfter should give time TAI of after given duration from now" {
                with(Mocks()) {
                    val dsl = object : TestTimeServiceDsl(this) {
                        fun testTaiAfter() {
                            val offset: Duration = taiTimeAfter(1.seconds).offsetFromNow()
                            checkWithIn(expected = 1.seconds, result = offset, tolerance = 2.milliseconds)
                        }
                    }

                    dsl.testTaiAfter()
                }
            }
        }
    }
}
