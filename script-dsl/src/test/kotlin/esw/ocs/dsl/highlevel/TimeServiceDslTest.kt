package esw.ocs.dsl.highlevel

import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import csw.time.scheduler.api.TimeServiceScheduler
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Job

class TimeServiceDslTest : WordSpec({
    class Mocks {
        val scheduler = mockk<TimeServiceScheduler>()
        val cancellable = mockk<Cancellable>()

        val startTime = UTCTime.now()
        val duration: Duration = 10.seconds
        val jDuration = duration.toJavaDuration()

        val timeServiceDsl = object : TimeServiceDsl {
            override val timeServiceScheduler: TimeServiceScheduler = scheduler
            override val coroutineContext: CoroutineContext = Job()
        }
    }

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
    }
})
