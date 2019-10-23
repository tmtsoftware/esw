//package esw.ocs.dsl.highlevel
//
//import akka.Done.done
//import csw.alarm.api.javadsl.IAlarmService
//import csw.alarm.api.javadsl.JAlarmSeverity.Major
//import csw.alarm.models.Key.AlarmKey
//import csw.params.javadsl.JSubsystem.TCS
//import io.kotlintest.eventually
//import io.kotlintest.seconds
//import io.mockk.every
//import io.mockk.mockk
//import io.mockk.verify
//import org.junit.jupiter.api.Test
//import java.util.concurrent.CompletableFuture.completedFuture
//
//class AlarmServiceDslTest {
//    @Test
//    fun `AlarmServiceDsl should set severity of alarms | ESW-125`() {
//        val mockedAlarmService: IAlarmService = mockk()
//        val alarmServiceDsl: AlarmServiceDsl = AlarmServiceDslImpl(mockedAlarmService)
//
//        val alarmKey = AlarmKey(TCS, "filter_assembly", "temperature")
//        val severity = Major()
//
//        every {
//            mockedAlarmService.setSeverity(alarmKey, severity)
//        } answers { completedFuture(done()) }
//
//        alarmServiceDsl.setSeverity(alarmKey, severity)
//
//        eventually(5.seconds) {
//            verify { mockedAlarmService.setSeverity(alarmKey, severity) }
//        }
//    }
//}
