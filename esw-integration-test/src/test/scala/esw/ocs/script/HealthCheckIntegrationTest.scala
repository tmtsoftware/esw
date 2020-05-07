package esw.ocs.script

import akka.actor.typed.ActorSystem
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.logging.models.Level.TRACE
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
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

class HealthCheckIntegrationTest extends EswTestKit {

  // HealthCheck.kts
  private val ocsSubsystem               = ESW
  private val ocsObservingMode           = "healthCheck"
  private var ocsSequencer: SequencerApi = _

  private val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  var loggingSystem: LoggingSystem                = _
  val heartbeatReceivedLog                        = "[StrandEC Heartbeat Received]"
  val heartbeatMissedLog: String = "[StrandEC Heartbeat Delayed] - Scheduled sending of heartbeat was delayed. " +
    "The reason can be thread starvation, e.g. by running blocking tasks in sequencer script, CPU overload, or GC."

  override def beforeEach(): Unit = {
    super.beforeEach()
    ocsSequencer = spawnSequencerProxy(ocsSubsystem, ocsObservingMode)
    logBuffer.clear()
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

  override def afterEach(): Unit = shutdownAllSequencers()

  override def afterAll(): Unit = {
    super.afterAll()
    loggingSystem.stop
  }

  "Sequencer Script" must {
    "receive heartbeat and receive heartbeat missed incase of blocking | ESW-290" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("nonblocking-command"), None)
      val command2 = Setup(Prefix("esw.test"), CommandName("blocking-command"), None)
      val sequence = Sequence(Seq(command1, command2))
      Await.result(ocsSequencer.submitAndWait(sequence), 5.seconds)

      val logs: mutable.Seq[String] = logBuffer.map(log => log.getString("message"))
      logs.count(_.equalsIgnoreCase(heartbeatReceivedLog)) shouldBe >=(2)
      logs.count(_.equalsIgnoreCase(heartbeatMissedLog)) shouldBe >=(2)
    }
  }
}
