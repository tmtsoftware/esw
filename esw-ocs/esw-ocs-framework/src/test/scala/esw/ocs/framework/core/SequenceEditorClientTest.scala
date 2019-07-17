package esw.ocs.framework.core

import akka.actor.testkit.typed.scaladsl.ActorTestKitBase
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.StepStatus.Pending
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.StepListError.{AddFailed, NotAllowedOnFinishedSeq, NotSupported, PauseFailed}
import esw.ocs.framework.api.models.{Sequence, StepList}

import scala.concurrent.Future

class SequenceEditorClientTest extends ActorTestKitBase with BaseTestSuite {

  case class TestData(testName: String, method: () => Future[Any], expectedResponse: Any)

  private val command      = Setup(Prefix("test"), CommandName("command-1"), None)
  private val status       = Pending
  private val notAllowed   = Left(NotAllowedOnFinishedSeq)
  private val notSupported = Left(NotSupported(status))

  private val getSequenceResponse      = StepList(Sequence(command)).toOption.get
  private val availableResponse        = true
  private val addResponse              = Left(AddFailed)
  private val pauseResponse            = Left(PauseFailed)
  private val prependResponse          = notAllowed
  private val resumeResponse           = notAllowed
  private val removeBreakpointResponse = notAllowed
  private val resetResponse            = notAllowed
  private val replaceResponse          = notSupported
  private val insertAfterResponse      = notSupported
  private val deleteResponse           = notSupported
  private val addBreakpointResponse    = notSupported

  private val mockedBehavior: Behaviors.Receive[ExternalSequencerMsg] = Behaviors.receiveMessage[ExternalSequencerMsg] { msg =>
    msg match {
      case GetSequence(replyTo)                                   => replyTo ! getSequenceResponse
      case Available(replyTo)                                     => replyTo ! availableResponse
      case Add(List(`command`), replyTo)                          => replyTo ! addResponse
      case Prepend(List(`command`), replyTo)                      => replyTo ! prependResponse
      case Replace(`command`.runId, List(`command`), replyTo)     => replyTo ! replaceResponse
      case InsertAfter(`command`.runId, List(`command`), replyTo) => replyTo ! insertAfterResponse
      case Delete(`command`.runId, replyTo)                       => replyTo ! deleteResponse
      case Pause(replyTo)                                         => replyTo ! pauseResponse
      case Resume(replyTo)                                        => replyTo ! resumeResponse
      case AddBreakpoint(`command`.runId, replyTo)                => replyTo ! addBreakpointResponse
      case RemoveBreakpoint(`command`.runId, replyTo)             => replyTo ! removeBreakpointResponse
      case Reset(replyTo)                                         => replyTo ! resetResponse
      case _                                                      =>
    }
    Behaviors.same
  }

  private val sequencer = spawn(mockedBehavior)

  private val sequenceEditorClient = new SequenceEditorClient(sequencer)

  private val testData = List(
    TestData("status", () => sequenceEditorClient.status, getSequenceResponse),
    TestData("isAvailable", () => sequenceEditorClient.isAvailable, availableResponse),
    TestData("add", () => sequenceEditorClient.add(List(command)), addResponse),
    TestData("prepend", () => sequenceEditorClient.prepend(List(command)), prependResponse),
    TestData("replace", () => sequenceEditorClient.replace(command.runId, List(command)), replaceResponse),
    TestData("insertAfter", () => sequenceEditorClient.insertAfter(command.runId, List(command)), insertAfterResponse),
    TestData("delete", () => sequenceEditorClient.delete(command.runId), deleteResponse),
    TestData("pause", () => sequenceEditorClient.pause, pauseResponse),
    TestData("resume", () => sequenceEditorClient.resume, resumeResponse),
    TestData("addBreakpoint", () => sequenceEditorClient.addBreakpoint(command.runId), addBreakpointResponse),
    TestData("removeBreakpoint", () => sequenceEditorClient.removeBreakpoint(command.runId), removeBreakpointResponse),
    TestData("reset", () => sequenceEditorClient.reset(), resetResponse)
  )

  testData.foreach { test =>
    test.testName in {
      test.method().futureValue should ===(test.expectedResponse)
    }
  }
}
