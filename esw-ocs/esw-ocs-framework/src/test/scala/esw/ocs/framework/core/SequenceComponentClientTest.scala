package esw.ocs.framework.core

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.messages.SequenceComponentMsg
import esw.ocs.framework.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SequenceComponentClientTest extends BaseTestSuite {

  private implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
  private implicit val scheduler: Scheduler                    = actorSystem.scheduler
  private implicit val timeout: Timeout                        = Timeout(10.seconds)

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }

  "SequenceComponentClient" must {

    "should delegate to LoadScript | ESW-103" in {
      val sequencerId             = "testSequencerId1"
      val observingMode           = "testObservingMode1"
      val sequenceComponentProbe  = TestProbe[SequenceComponentMsg]
      val sequenceComponentClient = new SequenceComponentClient(sequenceComponentProbe.ref)

      sequenceComponentClient.loadScript(sequencerId, observingMode)

      sequenceComponentProbe.expectMessageType[LoadScript]
    }

    "should delegate to GetStatus | ESW-103" in {
      val sequenceComponentProbe  = TestProbe[SequenceComponentMsg]
      val sequenceComponentClient = new SequenceComponentClient(sequenceComponentProbe.ref)

      sequenceComponentClient.getStatus

      sequenceComponentProbe.expectMessageType[GetStatus]
    }

    "should delegate to UnloadScript | ESW-103" in {
      val sequenceComponentProbe  = TestProbe[SequenceComponentMsg]
      val sequenceComponentClient = new SequenceComponentClient(sequenceComponentProbe.ref)

      sequenceComponentClient.unloadScript()

      sequenceComponentProbe.expectMessageType[UnloadScript]
    }
  }
}
