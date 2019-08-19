package esw.ocs.api.serializer

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.serialization.{SerializationExtension, Serializer}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands._
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.SequencerState.Idle
import esw.ocs.api.models.messages.EditorError._
import esw.ocs.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import esw.ocs.api.models.messages.SequenceComponentResponse.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._
import esw.ocs.api.models.{Step, StepList}
import org.scalatest.Assertion
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

class OcsAkkaSerializerTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private final val serialization = SerializationExtension(system.toUntyped)

  private val goOnlineResponseRed: ActorRef[GoOnlineResponse]           = TestProbe[GoOnlineResponse].ref
  private val okOrUnhandledResponseRef: ActorRef[OkOrUnhandledResponse] = TestProbe[OkOrUnhandledResponse].ref
  private val loadSequenceResponseRef: ActorRef[LoadSequenceResponse]   = TestProbe[LoadSequenceResponse].ref
  private val sequenceResponseRef: ActorRef[SequenceResponse]           = TestProbe[SequenceResponse].ref
  private val editorResponseRef: ActorRef[EswSequencerResponse]         = TestProbe[EswSequencerResponse].ref
  private val stepListResponseRef: ActorRef[StepListResponse]           = TestProbe[StepListResponse].ref
  private val stepListOptionResponseRef: ActorRef[StepListResponse]     = TestProbe[StepListResponse].ref
  private val loadScriptResponseRef: ActorRef[LoadScriptResponse]       = TestProbe[LoadScriptResponse].ref
  private val getStatusResponseRef: ActorRef[GetStatusResponse]         = TestProbe[GetStatusResponse].ref
  private val unloadScriptResponseRef: ActorRef[Done]                   = TestProbe[Done].ref
  private val setupCommand                                              = Setup(Prefix("esw.test"), CommandName("test"), None)
  private val steps: List[Step]                                         = List(Step(setupCommand))
  private val sequenceCommandList: List[SequenceCommand]                = List(setupCommand)

  "EswSequencerMessage" must {
    "use OcsAkkaSerializer for (de)serialization" in {
      case class Sample(i: Int) extends OcsAkkaSerializable
      val testData = Table(
        "EswSequencerMessage",
        LoadSequence(Sequence(setupCommand), loadSequenceResponseRef),
        StartSequence(sequenceResponseRef),
        GoOnline(goOnlineResponseRed),
        GoOffline(okOrUnhandledResponseRef),
        Shutdown(okOrUnhandledResponseRef),
        Abort(okOrUnhandledResponseRef),
        GetSequence(stepListResponseRef),
        GetPreviousSequence(stepListOptionResponseRef),
        Add(sequenceCommandList, editorResponseRef),
        Prepend(sequenceCommandList, editorResponseRef),
        Replace(Id(), sequenceCommandList, editorResponseRef),
        InsertAfter(Id(), sequenceCommandList, editorResponseRef),
        Delete(Id(), editorResponseRef),
        AddBreakpoint(Id(), editorResponseRef),
        RemoveBreakpoint(Id(), editorResponseRef),
        Pause(editorResponseRef),
        Resume(editorResponseRef),
        Reset(editorResponseRef)
      )

      forAll(testData) { assertSerde }
    }
  }

  "StepList" must {
    "use OcsAkkaSerializer for (de)serialization" in {
      assertSerde(StepList(Id(), steps))
    }
  }

  "SequenceComponentMsg" must {
    "use OcsAkkaSerializer for (de)serialization" in {
      val testData = Table(
        "SequenceComponentMsg models",
        LoadScript("sequencerId", "observingMode", loadScriptResponseRef),
        GetStatus(getStatusResponseRef),
        UnloadScript(unloadScriptResponseRef)
      )

      forAll(testData) { assertSerde }
    }
  }

  "SequenceComponentResponse" must {
    "use OcsAkkaSerializer for (de)serialization" in {
      val akkaLocation = AkkaLocation(
        AkkaConnection(ComponentId("testComponent", ComponentType.Sequencer)),
        Prefix("esw.test.component"),
        new URI("testURI")
      )
      val testData = Table(
        "SequenceComponentResponse models",
        LoadScriptResponse(Left(RegistrationError("error"))),
        LoadScriptResponse(Right(akkaLocation)),
        GetStatusResponse(Some(akkaLocation)),
        GetStatusResponse(None)
      )

      forAll(testData) { assertSerde }
    }
  }

  "EswSequencerResponse" must {
    "use OcsAkkaSerializer for (de)serialization" in {
      val stepList = StepList(Id(), steps)
      val step     = Step(setupCommand)
      val testData = Table(
        "EswSequencerResponse",
        Ok,
        StepListResult(Some(stepList)),
        PullNextResult(step),
        MaybeNextResult(Some(step)),
        SequenceResult(Completed(Id())),
        Unhandled(Idle, "GoOnline"),
        DuplicateIdsFound,
        GoOnlineHookFailed,
        CannotOperateOnAnInFlightOrFinishedStep,
        IdDoesNotExist(Id())
      )

      forAll(testData) { assertSerde }
    }
  }

  "RuntimeException" must {
    "be thrown for not supported models while (de)serialization" in {
      case class InternalMsg(msg: String) extends OcsAkkaSerializable

      intercept[RuntimeException] { assertSerde(InternalMsg("test")) }
    }
  }

  def assertSerde[T <: AnyRef](model: T): Assertion = {
    val serializer: Serializer = serialization.findSerializerFor(model)
    serializer.getClass shouldBe classOf[OcsAkkaSerializer]

    val bytes = serializer.toBinary(model)
    serializer.fromBinary(bytes, Some(model.getClass)) shouldEqual model
  }

}
