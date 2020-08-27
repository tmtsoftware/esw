package esw.ocs.api.client

import java.net.URI

import akka.util.Timeout
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.SequencerRequest._
import esw.ocs.api.protocol.SequencerStreamRequest.QueryFinal
import esw.ocs.api.protocol.{GoOnlineResponse, OkOrUnhandledResponse, SequencerRequest, _}
import esw.testcommons.BaseTestSuite
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class SequencerClientTest extends BaseTestSuite with SequencerServiceCodecs {

  private val postClient      = mock[Transport[SequencerRequest]]
  private val websocketClient = mock[Transport[SequencerStreamRequest]]
  private val sequencer       = new SequencerClient(postClient, websocketClient)

  "SequencerClient" must {

    "call postClient with GetSequence request | ESW-222, ESW-362" in {
      val maybeStepList = mock[Option[StepList]]
      when(
        postClient.requestResponse[Option[StepList]](argsEq(GetSequence))(
          any[Decoder[Option[StepList]]](),
          any[Encoder[Option[StepList]]]()
        )
      ).thenReturn(Future.successful(maybeStepList))
      sequencer.getSequence.futureValue should ===(maybeStepList)
    }

    "call postClient with IsAvailable request which returns true | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsAvailable))(any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(true))
      sequencer.isAvailable.futureValue should ===(true)
    }

    "call postClient with IsAvailable request which returns false | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsAvailable))(any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(false))
      sequencer.isAvailable.futureValue should ===(false)
    }

    "call postClient with IsOnline request which returns true | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsOnline))(any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(true))
      sequencer.isOnline.futureValue should ===(true)
    }

    "call postClient with IsOnline request which returns false | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsOnline))(any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(false))
      sequencer.isOnline.futureValue should ===(false)
    }

    "call postClient with Add request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Add(List.empty)))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.add(List.empty).futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with Prepend request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Prepend(List.empty)))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.prepend(List.empty).futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with Replace request | ESW-222, ESW-362" in {
      val genericResponse = mock[GenericResponse]
      val id              = mock[Id]
      when(
        postClient.requestResponse[GenericResponse](argsEq(Replace(id, List.empty)))(
          any[Decoder[GenericResponse]](),
          any[Encoder[GenericResponse]]()
        )
      ).thenReturn(Future.successful(genericResponse))
      sequencer.replace(id, List.empty).futureValue should ===(genericResponse)
    }

    "call postClient with InsertAfter request | ESW-222, ESW-362" in {
      val genericResponse = mock[GenericResponse]
      val id              = mock[Id]
      when(
        postClient.requestResponse[GenericResponse](argsEq(InsertAfter(id, List.empty)))(
          any[Decoder[GenericResponse]](),
          any[Encoder[GenericResponse]]()
        )
      ).thenReturn(Future.successful(genericResponse))
      sequencer.insertAfter(id, List.empty).futureValue should ===(genericResponse)
    }

    "call postClient with Delete request | ESW-222, ESW-362" in {
      val genericResponse = mock[GenericResponse]
      val id              = mock[Id]
      when(
        postClient
          .requestResponse[GenericResponse](argsEq(Delete(id)))(any[Decoder[GenericResponse]](), any[Encoder[GenericResponse]]())
      ).thenReturn(Future.successful(genericResponse))
      sequencer.delete(id).futureValue should ===(genericResponse)
    }

    "call postClient with Pause request | ESW-222, ESW-362" in {
      val pauseResponse = mock[PauseResponse]
      when(postClient.requestResponse[PauseResponse](argsEq(Pause))(any[Decoder[PauseResponse]](), any[Encoder[PauseResponse]]()))
        .thenReturn(Future.successful(pauseResponse))
      sequencer.pause.futureValue should ===(pauseResponse)
    }

    "call postClient with Resume request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Resume))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.resume.futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with AddBreakpoint request | ESW-222, ESW-362" in {
      val genericResponse = mock[GenericResponse]
      val id              = mock[Id]
      when(
        postClient.requestResponse[GenericResponse](argsEq(AddBreakpoint(id)))(
          any[Decoder[GenericResponse]](),
          any[Encoder[GenericResponse]]()
        )
      ).thenReturn(Future.successful(genericResponse))
      sequencer.addBreakpoint(id).futureValue should ===(genericResponse)
    }

    "call postClient with RemoveBreakpoint request | ESW-222, ESW-362" in {
      val removeBreakpointResponse = mock[RemoveBreakpointResponse]
      val id                       = mock[Id]
      when(
        postClient
          .requestResponse[RemoveBreakpointResponse](argsEq(RemoveBreakpoint(id)))(
            any[Decoder[RemoveBreakpointResponse]](),
            any[Encoder[RemoveBreakpointResponse]]()
          )
      ).thenReturn(Future.successful(removeBreakpointResponse))
      sequencer.removeBreakpoint(id).futureValue should ===(removeBreakpointResponse)
    }

    "call postClient with Reset request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Reset))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.reset().futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with AbortSequence request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(AbortSequence))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.abortSequence().futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with Stop request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Stop))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.stop().futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with GoOffline request | ESW-222, ESW-101, ESW-362" in {
      val goOfflineResponse = mock[GoOfflineResponse]
      when(
        postClient.requestResponse[GoOfflineResponse](argsEq(GoOffline))(
          any[Decoder[GoOfflineResponse]](),
          any[Encoder[GoOfflineResponse]]()
        )
      ).thenReturn(Future.successful(goOfflineResponse))
      sequencer.goOffline().futureValue should ===(goOfflineResponse)
    }

    "call postClient with GoOnline request | ESW-222, ESW-101, ESW-362" in {
      val goOnlineResponse = mock[GoOnlineResponse]
      when(
        postClient
          .requestResponse[GoOnlineResponse](argsEq(GoOnline))(any[Decoder[GoOnlineResponse]](), any[Encoder[GoOnlineResponse]]())
      ).thenReturn(Future.successful(goOnlineResponse))
      sequencer.goOnline().futureValue should ===(goOnlineResponse)
    }

    "call postClient with LoadSequence request | ESW-222, ESW-101, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      val command               = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence              = Sequence(command)
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(LoadSequence(sequence)))(
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.loadSequence(sequence).futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with StartSequence request | ESW-222, ESW-101, ESW-362" in {
      val submitResponse = mock[SubmitResponse]
      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(StartSequence))(any[Decoder[SubmitResponse]](), any[Encoder[SubmitResponse]]())
      ).thenReturn(Future.successful(submitResponse))
      sequencer.startSequence().futureValue should ===(submitResponse)
    }

    "call postClient with SubmitSequence request | ESW-222, ESW-101, ESW-362" in {
      val command        = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence       = Sequence(command)
      val submitResponse = mock[SubmitResponse]
      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(Submit(sequence)))(
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(submitResponse))
      sequencer.submit(sequence).futureValue should ===(submitResponse)
    }

    "call postClient with SubmitAndWait request | ESW-222, ESW-101" in {
      val command1                  = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence                  = Sequence(command1)
      val sequenceId                = Id()
      val startedResponse           = Started(sequenceId)
      val completedResponse         = Completed(sequenceId)
      implicit val timeout: Timeout = Timeout(10.seconds)

      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(Submit(sequence)))(
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(startedResponse))

      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(sequenceId, timeout)), any[FiniteDuration]())(
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(completedResponse))

      sequencer.submitAndWait(sequence).futureValue should ===(completedResponse)
    }

    "call postClient with DiagnosticMode request | ESW-143, ESW-101, ESW-362" in {
      val diagnosticModeResponse = mock[DiagnosticModeResponse]
      val startTime              = UTCTime.now()
      val hint                   = "engineering"
      when(
        postClient.requestResponse[DiagnosticModeResponse](argsEq(DiagnosticMode(startTime, hint)))(
          any[Decoder[DiagnosticModeResponse]](),
          any[Encoder[DiagnosticModeResponse]]()
        )
      ).thenReturn(Future.successful(diagnosticModeResponse))
      sequencer.diagnosticMode(startTime, hint).futureValue should ===(diagnosticModeResponse)
    }

    "call postClient with OperationsMode request | ESW-143, ESW-101, ESW-362" in {
      val operationsModeResponse = mock[OperationsModeResponse]
      when(
        postClient.requestResponse[OperationsModeResponse](argsEq(OperationsMode))(
          any[Decoder[OperationsModeResponse]](),
          any[Encoder[OperationsModeResponse]]()
        )
      ).thenReturn(Future.successful(operationsModeResponse))
      sequencer.operationsMode().futureValue should ===(operationsModeResponse)
    }

    "call websocket with QueryFinal request | ESW-222, ESW-101, ESW-362" in {
      val id                        = mock[Id]
      val submitResponse            = mock[SubmitResponse]
      implicit val timeout: Timeout = Timeout(10.seconds)
      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(id, timeout)), any[FiniteDuration]())(
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(submitResponse))
      sequencer.queryFinal(id).futureValue should ===(submitResponse)
    }

    "call postClient with GetSequenceComponent request | ESW-222, ESW-255" in {
      val sequenceComponentLocation =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(randomSubsystem, "primary"), SequenceComponent)),
          new URI("mock-uri"),
          Metadata.empty
        )

      when(
        postClient
          .requestResponse[AkkaLocation](argsEq(GetSequenceComponent))(any[Decoder[AkkaLocation]](), any[Encoder[AkkaLocation]]())
      ).thenReturn(Future.successful(sequenceComponentLocation))
      sequencer.getSequenceComponent.futureValue should ===(sequenceComponentLocation)
    }
  }
}
