package esw.gateway.server.routes

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.params.core.models.Subsystem
import esw.gateway.server.CswContextMocks
import esw.gateway.server.requests.SetSeverity
import esw.http.core.HttpTestSuite

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class AlarmRoutesTest extends HttpTestSuite {
  val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  trait Setup {
    val cswMocks                  = new CswContextMocks(actorSystem)
    implicit val timeout: Timeout = 5.seconds
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  s"POST /alarm/current-severity/{subsystem}/{component}/{alarmName}" must {

    "set the current severity of alarm and return OK | ESW-193" in new Setup {

      import cswMocks._

      val componentName = "testComponent"
      val alarmName     = "testAlarmName"
      val subsystemName = Subsystem.IRIS
      val majorSeverity = AlarmSeverity.Major
      val alarmKey      = AlarmKey(subsystemName, componentName, alarmName)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.successful(Done))

      Post(s"/alarm/current-severity/$subsystemName/$componentName/$alarmName", SetSeverity(majorSeverity)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      verify(alarmService).setSeverity(alarmKey, majorSeverity)
    }

    "return 400-BadRequest when subsystem is invalid | ESW-193" in new Setup {

      import cswMocks._

      val componentName = "testComponent"
      val alarmName     = "testAlarmName"
      val majorSeverity = AlarmSeverity.Major

      Post(s"/alarm/current-severity/INVALID/$componentName/$alarmName", SetSeverity(majorSeverity)) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return 400-BadRequest when component name or alarm name is invalid | ESW-193" in new Setup {

      import cswMocks._

      val componentName = "testComponent"
      val alarmName     = "testAlarmName"
      val subsystemName = Subsystem.IRIS
      val majorSeverity = AlarmSeverity.Major
      val alarmKey      = AlarmKey(subsystemName, componentName, alarmName)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.failed(KeyNotFoundException(alarmKey)))

      Post(s"/alarm/current-severity/$subsystemName/$componentName/$alarmName", SetSeverity(majorSeverity)) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return 500-InternalServerError when setting severity fails | ESW-193" in new Setup {

      import cswMocks._

      val componentName = "testComponent"
      val alarmName     = "testAlarmName"
      val subsystemName = Subsystem.IRIS
      val majorSeverity = AlarmSeverity.Major
      val alarmKey      = AlarmKey(subsystemName, componentName, alarmName)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.failed(new RuntimeException("some error")))

      Post(s"/alarm/current-severity/$subsystemName/$componentName/$alarmName", SetSeverity(majorSeverity)) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }
}
