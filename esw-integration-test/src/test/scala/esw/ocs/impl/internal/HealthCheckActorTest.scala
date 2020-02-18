package esw.ocs.impl.internal

import java.time.Duration

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.{LoggerFactory, LoggingSystemFactory}
import csw.logging.models.Level.TRACE
import csw.prefix.models.Prefix
import esw.ocs.impl.messages.HealthCheckMsg
import esw.ocs.impl.messages.HealthCheckMsg.Heartbeat
import esw.ocs.testkit.BaseTestSuite
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class TestAppender(callback: Any => Unit) extends LogAppenderBuilder {

  /**
   * A constructor for the TestAppender class.
   *
   * @param system    typed Actor System.
   * @param stdHeaders the headers that are fixes for this service.
   * @return the stdout appender.
   */
  def apply(system: ActorSystem[_], stdHeaders: JsObject): StdOutAppender =
    new StdOutAppender(system, stdHeaders, callback)
}

class HealthCheckActorTest extends BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  implicit val timeout: Timeout                                = 5.seconds
  private val logBuffer: mutable.Buffer[JsObject]              = mutable.Buffer.empty[JsObject]
  var loggingSystem: LoggingSystem                             = _

  def spawnHealthCheckActor(healthCheckInterval: Duration): ActorRef[HealthCheckMsg] = {
    val logger           = new LoggerFactory(Prefix("esw.test")).getLogger
    val healthCheckActor = new HealthCheckActor(logger, healthCheckInterval)
    val actorRef: ActorRef[HealthCheckMsg] =
      (Await.result((actorSystem ? (Spawn(healthCheckActor.behavior(), "actor", Props.empty, _))), 5.seconds))
    actorRef
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    val testAppender = new TestAppender(x => {
      val jsObject = Json.parse(x.toString).as[JsObject]
      logBuffer += jsObject
    })
    loggingSystem = LoggingSystemFactory.forTestingOnly()
    loggingSystem.setAppenders(List(testAppender))
    loggingSystem.setDefaultLogLevel(TRACE)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    logBuffer.clear()
  }

  override def afterAll(): Unit = {
    loggingSystem.stop
  }

  "HealthCheckActor" must {
    "receive [Heartbeat] and [HeartbeatMissed] | ESW-290" in {
      val heartbeatInterval            = Duration.ofSeconds(3)
      val actorRef                     = spawnHealthCheckActor(heartbeatInterval)
      val intervalToExpectNotification = heartbeatInterval.toScala
      val heartbeatReceivedLog         = "[StrandEC Heartbeat Received]"
      val heartbeatMissedLog = "[StrandEC Heartbeat Delayed] - Scheduled sending of heartbeat was delayed. " +
        "The reason can be thread starvation, e.g. by running blocking tasks in sequencer script, CPU overload, or GC."

      //send heartbeat
      actorRef ! Heartbeat

      // sleep for heartbeat-interval
      Thread.sleep(intervalToExpectNotification.toMillis)
      val logs: mutable.Seq[String] = logBuffer.map(log => log.getString("message"))
      // assert heartbeat received log
      logs.contains(heartbeatReceivedLog) shouldBe true
      // assert heartbeat missed log not received
      logs.contains(heartbeatMissedLog) shouldBe false

      //sleep for heartbeat-interval
      Thread.sleep(intervalToExpectNotification.toMillis)
      val nextLogs: mutable.Seq[String] = logBuffer.map(log => log.getString("message"))
      //assert heartbeat missed log is received
      nextLogs.contains(heartbeatMissedLog) shouldBe true
    }
  }
}
