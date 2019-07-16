package esw.integration.test.ocs.framework.core

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.testkit.LocationTestKit
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.messages.SequenceComponentMsg
import esw.ocs.framework.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import esw.ocs.framework.core.SequenceComponentBehavior
import esw.ocs.framework.exceptions.SequencerAlreadyRunningException

import scala.concurrent.duration.DurationLong
import scala.util.Try

class SequenceComponentBehaviorTest extends BaseTestSuite {
  private val testKit                                = LocationTestKit()
  implicit val system: ActorSystem[_]                = ActorSystem(Behaviors.empty, "test")
  implicit val testKitSettings: TestKitSettings      = TestKitSettings(system)
  implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
  implicit val mat: ActorMaterializer                = ActorMaterializer()

  override def beforeAll(): Unit = {
    testKit.startLocationServer()
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools().await
    testKit.shutdownLocationServer()
    system.terminate()
    system.whenTerminated.await
  }

  private def createBehaviorTestKit(): BehaviorTestKit[SequenceComponentMsg] = BehaviorTestKit(
    Behaviors.setup[SequenceComponentMsg](_ => SequenceComponentBehavior.behavior)
  )

  "SequenceComponentBehavior" must {
    "should be able to load/unload script and get appropriate status | ESW-103" in {
      val behaviorTestKit         = createBehaviorTestKit()
      val loadScriptResponseProbe = TestProbe[Try[AkkaLocation]]
      val getStatusProbe          = TestProbe[Option[AkkaLocation]]
      val sequencerId             = "testSequencerId1"
      val observingMode           = "testObservingMode1"

      //LoadScript
      behaviorTestKit.run(LoadScript(sequencerId, observingMode, loadScriptResponseProbe.ref))

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationReponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.get
      loadScriptLocationReponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //GetStatus
      behaviorTestKit.run(GetStatus(getStatusProbe.ref))

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusLocationResponse: Location = getStatusProbe.receiveMessage(5.seconds).get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //UnloadScript
      behaviorTestKit.run(UnloadScript(TestProbe[Done].ref))

      //assert if GetStatus returns None after unloading sequencer script
      behaviorTestKit.run(GetStatus(getStatusProbe.ref))
      getStatusProbe.expectMessage(None)
    }

    "should throw SequencerAlreadyRunningException | ESW-103" in {
      val behaviorTestKit         = createBehaviorTestKit()
      val loadScriptResponseProbe = TestProbe[Try[AkkaLocation]]
      val sequencerId             = "testSequencerId2"
      val observingMode           = "testObservingMode2"

      //LoadScript
      behaviorTestKit.run(LoadScript(sequencerId, observingMode, loadScriptResponseProbe.ref))

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationReponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.get
      loadScriptLocationReponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //LoadScript should throw SequencerAlreadyRunningException
      behaviorTestKit.run(LoadScript("sequencerId3", "observingMode3", loadScriptResponseProbe.ref))
      intercept[SequencerAlreadyRunningException.type] {
        loadScriptResponseProbe.receiveMessage.get
      }
    }

    "UnloadScript should return done if sequence component not running any sequencer | ESW-103" in {
      val behaviorTestKit           = createBehaviorTestKit()
      val unloadScriptResponseProbe = TestProbe[Done]
      val getStatusProbe            = TestProbe[Option[AkkaLocation]]

      //assert if GetStatus returns None after unloading sequencer script
      behaviorTestKit.run(GetStatus(getStatusProbe.ref))
      getStatusProbe.expectMessage(None)

      //UnloadScript
      behaviorTestKit.run(UnloadScript(unloadScriptResponseProbe.ref))

      //Assert if UnloadScript returns Done
      unloadScriptResponseProbe.expectMessage(Done)
    }
  }
}
