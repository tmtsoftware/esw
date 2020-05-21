package esw.ocs.api.actor

import java.net.URI
import java.time.Instant

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.serialization.SerializationExtension
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.time.core.models.UTCTime
import esw.ocs.api.actor.messages.SequenceComponentMsg.{GetStatus, LoadScript, Restart, UnloadScript}
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState
import esw.ocs.api.actor.messages.SequencerState._
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol.EditorError.IdDoesNotExist
import esw.ocs.api.protocol.{DiagnosticModeResponse, SequencerSubmitResponse, _}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class OcsAkkaSerializerTest extends AnyWordSpecLike with Matchers with TypeCheckedTripleEquals with BeforeAndAfterAll {
  private final implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "OcsAkkaSerializerTest")
  private final val serialization                                       = SerializationExtension(system)

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  "should use ocs serializer for EswSequencerRemoteMessage (de)serialization" in {
    val pauseResponseRef            = TestProbe[PauseResponse]().ref
    val genericResponseRef          = TestProbe[GenericResponse]().ref
    val stepListRef                 = TestProbe[Option[StepList]]().ref
    val akkaLocationRef             = TestProbe[AkkaLocation]().ref
    val sequencerStateRef           = TestProbe[SequencerState[SequencerMsg]]().ref
    val goOfflineResponseRef        = TestProbe[GoOfflineResponse]().ref
    val goOnlineResponseRef         = TestProbe[GoOnlineResponse]().ref
    val operationsModeResponseRef   = TestProbe[OperationsModeResponse]().ref
    val pullNextResponseRef         = TestProbe[PullNextResponse]().ref
    val removeBreakpointResponseRef = TestProbe[RemoveBreakpointResponse]().ref
    val okTypeRef                   = TestProbe[Ok.type]().ref
    val sequencerSubmitResponseRef  = TestProbe[SequencerSubmitResponse]().ref
    val okOrUnhandledResponseRef    = TestProbe[OkOrUnhandledResponse]().ref
    val diagnosticModeResponseRef   = TestProbe[DiagnosticModeResponse]().ref
    val id                          = Id("id")
    val setup                       = Setup(Prefix("esw.test"), CommandName("command"), None)
    val sequence                    = Sequence(setup)
    val commands                    = List(setup)
    val startTime                   = UTCTime(Instant.ofEpochMilli(1000L))

    val testData = Table(
      "EswSequencerRemoteMessage models",
      AbortSequence(okOrUnhandledResponseRef),
      Stop(okOrUnhandledResponseRef),
      Pause(pauseResponseRef),
      Resume(okOrUnhandledResponseRef),
      AbortSequenceComplete(okOrUnhandledResponseRef),
      Add(commands, okOrUnhandledResponseRef),
      AddBreakpoint(id, genericResponseRef),
      Delete(id, genericResponseRef),
      DiagnosticMode(startTime, "hint", diagnosticModeResponseRef),
      GetSequence(stepListRef),
      GetSequenceComponent(akkaLocationRef),
      GetSequencerState(sequencerStateRef),
      GoIdle(okOrUnhandledResponseRef),
      GoOffline(goOfflineResponseRef),
      GoOfflineSuccess(goOfflineResponseRef),
      GoOfflineFailed(goOfflineResponseRef),
      GoOnline(goOnlineResponseRef),
      GoOnlineSuccess(goOnlineResponseRef),
      GoOnlineFailed(goOnlineResponseRef),
      InsertAfter(id, commands, genericResponseRef),
      LoadSequence(sequence, okOrUnhandledResponseRef),
      OperationsMode(operationsModeResponseRef),
      Prepend(commands, okOrUnhandledResponseRef),
      PullNext(pullNextResponseRef),
      RemoveBreakpoint(id, removeBreakpointResponseRef),
      Replace(id, commands, genericResponseRef),
      Reset(okOrUnhandledResponseRef),
      Shutdown(okTypeRef),
      ShutdownComplete(okTypeRef),
      StartSequence(sequencerSubmitResponseRef),
      StartingFailed(sequencerSubmitResponseRef),
      StartingSuccessful(sequencerSubmitResponseRef),
      StepFailure("reason", okOrUnhandledResponseRef),
      StepSuccess(okOrUnhandledResponseRef),
      Stop(okOrUnhandledResponseRef),
      StopComplete(okOrUnhandledResponseRef),
      SubmitFailed(sequencerSubmitResponseRef),
      SubmitSequenceInternal(sequence, sequencerSubmitResponseRef),
      SubmitSuccessful(sequence, sequencerSubmitResponseRef)
    )

    forAll(testData) { sequencerRemoteMsg =>
      val serializer = serialization.findSerializerFor(sequencerRemoteMsg)
      serializer.getClass shouldBe classOf[OcsAkkaSerializer]

      val bytes = serializer.toBinary(sequencerRemoteMsg)
      serializer.fromBinary(bytes, Some(sequencerRemoteMsg.getClass)) shouldEqual sequencerRemoteMsg
    }
  }

  "should use ocs serializer for EswSequencerResponse (de)serialization" in {
    val id      = Id("id")
    val message = "message"
    val setup   = Setup(Prefix("esw.test"), CommandName("command"), None)

    val testData = Table(
      "EswSequencerRemoteMessage models",
      DiagnosticHookFailed(message),
      GoOfflineHookFailed(message),
      GoOnlineHookFailed(message),
      IdDoesNotExist(id),
      NewSequenceHookFailed(message),
      OperationsHookFailed(message),
      PullNextResult(Step(setup)),
      SubmitResult(Completed(id)),
      Unhandled("state", "messageType")
    )

    forAll(testData) { sequencerResponse =>
      val serializer = serialization.findSerializerFor(sequencerResponse)
      serializer.getClass shouldBe classOf[OcsAkkaSerializer]

      val bytes = serializer.toBinary(sequencerResponse)
      serializer.fromBinary(bytes, Some(sequencerResponse.getClass)) shouldEqual sequencerResponse
    }
  }

  "should use ocs serializer for StepList (de)serialization" in {
    val setup    = Setup(Prefix("esw.test"), CommandName("command"), None)
    val sequence = Sequence(setup)
    val stepList = StepList(sequence)

    val serializer = serialization.findSerializerFor(stepList)
    serializer.getClass shouldBe classOf[OcsAkkaSerializer]

    val bytes = serializer.toBinary(stepList)
    serializer.fromBinary(bytes, Some(stepList.getClass)) shouldEqual stepList
  }

  "should use ocs serializer for Response (de)serialization" in {
    val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Sequencer)), URI.create("uri"))
    val testData = Table(
      "Sequencer Response models",
      GetStatusResponse(Some(akkaLocation)),
      GetStatusResponse(None),
      ScriptResponse(Left(ScriptError("error"))),
      ScriptResponse(Right(akkaLocation))
    )

    forAll(testData) { response =>
      val serializer = serialization.findSerializerFor(response)
      serializer.getClass shouldBe classOf[OcsAkkaSerializer]

      val bytes = serializer.toBinary(response)
      serializer.fromBinary(bytes, Some(response.getClass)) shouldEqual response
    }
  }

  "should use ocs serializer for SequenceComponentMsg (de)serialization" in {
    val testData = Table(
      "SequenceComponentMsg models",
      LoadScript(ESW, "IRIS_Darknight", TestProbe[ScriptResponse]().ref),
      Restart(TestProbe[ScriptResponse]().ref),
      UnloadScript(TestProbe[Done]().ref),
      GetStatus(TestProbe[GetStatusResponse]().ref)
    )

    forAll(testData) { sequenceComponentMsg =>
      val serializer = serialization.findSerializerFor(sequenceComponentMsg)
      serializer.getClass shouldBe classOf[OcsAkkaSerializer]

      val bytes = serializer.toBinary(sequenceComponentMsg)
      serializer.fromBinary(bytes, Some(sequenceComponentMsg.getClass)) shouldEqual sequenceComponentMsg
    }
  }

  "should use ocs serializer for SequencerState (de)serialization" in {
    val testData = Table(
      "SequencerState models",
      Idle,
      Loaded,
      InProgress,
      Offline,
      GoingOnline,
      GoingOffline,
      ShuttingDown,
      AbortingSequence,
      Stopping,
      Submitting,
      Starting
    )

    forAll(testData) { sequencerState =>
      val serializer = serialization.findSerializerFor(sequencerState)
      serializer.getClass shouldBe classOf[OcsAkkaSerializer]

      val bytes = serializer.toBinary(sequencerState)
      serializer.fromBinary(bytes, Some(sequencerState.getClass)) shouldEqual sequencerState
    }
  }
}
