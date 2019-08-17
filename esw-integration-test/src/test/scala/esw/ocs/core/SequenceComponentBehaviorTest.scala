package esw.ocs.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.messages.SequenceComponentMsg._
import esw.ocs.api.models.messages.SequenceComponentResponse.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.messages.{RegistrationError, SequenceComponentMsg}

import scala.concurrent.duration.DurationLong

class SequenceComponentBehaviorTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._
  private implicit val typedSystem: ActorSystem[_] = actorSystem
  val ocsSequenceComponentName                     = "OCS_1"

  private def createBehaviorTestKit(): BehaviorTestKit[SequenceComponentMsg] = BehaviorTestKit(
    Behaviors.setup[SequenceComponentMsg] { _ =>
      SequenceComponentBehavior.behavior(ocsSequenceComponentName)
    }
  )

  "SequenceComponentBehavior" must {
    "load/unload script and get appropriate status | ESW-103" in {
      val behaviorTestKit         = createBehaviorTestKit()
      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val getStatusProbe          = TestProbe[GetStatusResponse]
      val sequencerId             = "testSequencerId1"
      val observingMode           = "testObservingMode1"

      //LoadScript
      behaviorTestKit.run(LoadScript(sequencerId, observingMode, loadScriptResponseProbe.ref))

      //todo: try resolving from location service
      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //GetStatus
      behaviorTestKit.run(GetStatus(getStatusProbe.ref))

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusLocationResponse: Location = getStatusProbe.receiveMessage(5.seconds).response.get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //UnloadScript
      behaviorTestKit.run(UnloadScript(TestProbe[Done].ref))

      //assert if GetStatus returns None after unloading sequencer script
      behaviorTestKit.run(GetStatus(getStatusProbe.ref))
      getStatusProbe.expectMessage(GetStatusResponse(None))
    }

    "load script and give LoadScriptError if sequencer is already running | ESW-103" in {
      val behaviorTestKit         = createBehaviorTestKit()
      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val sequencerId             = "testSequencerId2"
      val observingMode           = "testObservingMode2"

      //LoadScript
      behaviorTestKit.run(LoadScript(sequencerId, observingMode, loadScriptResponseProbe.ref))

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      behaviorTestKit.run(LoadScript("sequencerId3", "observingMode3", loadScriptResponseProbe.ref))
      loadScriptResponseProbe.receiveMessage.response.leftValue shouldBe RegistrationError(
        "Loading script failed: Sequencer already running"
      )
    }

    "unload script and return Done if sequence component is not running any sequencer | ESW-103" in {
      val behaviorTestKit           = createBehaviorTestKit()
      val unloadScriptResponseProbe = TestProbe[Done]
      val getStatusProbe            = TestProbe[GetStatusResponse]

      //assert if GetStatus returns None after unloading sequencer script
      behaviorTestKit.run(GetStatus(getStatusProbe.ref))
      getStatusProbe.expectMessage(GetStatusResponse(None))

      //UnloadScript
      behaviorTestKit.run(UnloadScript(unloadScriptResponseProbe.ref))

      //Assert if UnloadScript returns Done
      unloadScriptResponseProbe.expectMessage(Done)
    }

    "get killed if stop msg is received | ESW-103, ESW-214" in {
      val behaviorTestKit = createBehaviorTestKit()

      behaviorTestKit.run(Stop)

      behaviorTestKit.isAlive shouldEqual false
    }
  }
}
