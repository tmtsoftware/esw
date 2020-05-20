package esw.ocs.api.actor

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.serialization.SerializationExtension
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.AkkaLocation
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState
import esw.ocs.api.models.StepList
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
    val sequence                    = Sequence(Seq(setup))
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
}
