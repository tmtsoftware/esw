package esw.ocs.api.client

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Metadata}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.models.{SequencerState, StepList}
import esw.ocs.api.protocol.SequencerRequest.*
import esw.ocs.api.protocol.SequencerStreamRequest.{QueryFinal, SubscribeSequencerState}
import esw.ocs.api.protocol.*
import esw.testcommons.BaseTestSuite
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import msocket.jvm.SourceExtension.RichSource
import org.mockito.ArgumentMatchers.{any, eq => argsEq}
import org.mockito.Mockito.when

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class SequencerClientTest extends BaseTestSuite with SequencerServiceCodecs {

  private val postClient      = mock[Transport[SequencerRequest]]
  private val websocketClient = mock[Transport[SequencerStreamRequest]]
  private val sequencer       = new SequencerClient(postClient, websocketClient)

  "SequencerClient" must {

    "call websocketClient with SubscribeSequencerState request  | ESW-213" in {

      val source = Source.empty[SequencerStateResponse].withSubscription()
      when(
        websocketClient.requestStream[SequencerStateResponse](argsEq(SubscribeSequencerState))(using
          any[Decoder[SequencerStateResponse]](),
          any[Encoder[SequencerStateResponse]]()
        )
      ).thenReturn(source)

      sequencer.subscribeSequencerState() should ===(source)
    }

    "call postClient with GetSequence request | ESW-222, ESW-362" in {
      val maybeStepList = mock[Option[StepList]]
      when(
        postClient.requestResponse[Option[StepList]](argsEq(GetSequence))(using
          any[Decoder[Option[StepList]]](),
          any[Encoder[Option[StepList]]]()
        )
      ).thenReturn(Future.successful(maybeStepList))
      sequencer.getSequence.futureValue should ===(maybeStepList)
    }

    "call postClient with IsAvailable request which returns true | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsAvailable))(using any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(true))
      sequencer.isAvailable.futureValue should ===(true)
    }

    "call postClient with IsAvailable request which returns false | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsAvailable))(using any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(false))
      sequencer.isAvailable.futureValue should ===(false)
    }

    "call postClient with IsOnline request which returns true | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsOnline))(using any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(true))
      sequencer.isOnline.futureValue should ===(true)
    }

    "call postClient with IsOnline request which returns false | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsOnline))(using any[Decoder[Boolean]](), any[Encoder[Boolean]]()))
        .thenReturn(Future.successful(false))
      sequencer.isOnline.futureValue should ===(false)
    }

    "call postClient with Add request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Add(List.empty)))(using
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.add(List.empty).futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with Prepend request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Prepend(List.empty)))(using
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
        postClient.requestResponse[GenericResponse](argsEq(Replace(id, List.empty)))(using
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
        postClient.requestResponse[GenericResponse](argsEq(InsertAfter(id, List.empty)))(using
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
          .requestResponse[GenericResponse](argsEq(Delete(id)))(using
            any[Decoder[GenericResponse]](),
            any[Encoder[GenericResponse]]()
          )
      ).thenReturn(Future.successful(genericResponse))
      sequencer.delete(id).futureValue should ===(genericResponse)
    }

    "call postClient with Pause request | ESW-222, ESW-362" in {
      val pauseResponse = mock[PauseResponse]
      when(
        postClient.requestResponse[PauseResponse](argsEq(Pause))(using
          any[Decoder[PauseResponse]](),
          any[Encoder[PauseResponse]]()
        )
      )
        .thenReturn(Future.successful(pauseResponse))
      sequencer.pause.futureValue should ===(pauseResponse)
    }

    "call postClient with Resume request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Resume))(using
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
        postClient.requestResponse[GenericResponse](argsEq(AddBreakpoint(id)))(using
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
          .requestResponse[RemoveBreakpointResponse](argsEq(RemoveBreakpoint(id)))(using
            any[Decoder[RemoveBreakpointResponse]](),
            any[Encoder[RemoveBreakpointResponse]]()
          )
      ).thenReturn(Future.successful(removeBreakpointResponse))
      sequencer.removeBreakpoint(id).futureValue should ===(removeBreakpointResponse)
    }

    "call postClient with Reset request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Reset))(using
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.reset().futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with AbortSequence request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(AbortSequence))(using
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.abortSequence().futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with Stop request | ESW-222, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(Stop))(using
          any[Decoder[OkOrUnhandledResponse]](),
          any[Encoder[OkOrUnhandledResponse]]()
        )
      ).thenReturn(Future.successful(okOrUnhandledResponse))
      sequencer.stop().futureValue should ===(okOrUnhandledResponse)
    }

    "call postClient with GoOffline request | ESW-222, ESW-101, ESW-362" in {
      val goOfflineResponse = mock[GoOfflineResponse]
      when(
        postClient.requestResponse[GoOfflineResponse](argsEq(GoOffline))(using
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
          .requestResponse[GoOnlineResponse](argsEq(GoOnline))(using
            any[Decoder[GoOnlineResponse]](),
            any[Encoder[GoOnlineResponse]]()
          )
      ).thenReturn(Future.successful(goOnlineResponse))
      sequencer.goOnline().futureValue should ===(goOnlineResponse)
    }

    "call postClient with LoadSequence request | ESW-222, ESW-101, ESW-362" in {
      val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
      val command               = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence              = Sequence(command)
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(LoadSequence(sequence)))(using
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
          .requestResponse[SubmitResponse](argsEq(StartSequence))(using
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(submitResponse))
      sequencer.startSequence().futureValue should ===(submitResponse)
    }

    "call postClient with SubmitSequence request | ESW-222, ESW-101, ESW-362" in {
      val command        = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence       = Sequence(command)
      val submitResponse = mock[SubmitResponse]
      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(Submit(sequence)))(using
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
          .requestResponse[SubmitResponse](argsEq(Submit(sequence)))(using
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(startedResponse))

      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(sequenceId, timeout)), any[FiniteDuration]())(using
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
        postClient.requestResponse[DiagnosticModeResponse](argsEq(DiagnosticMode(startTime, hint)))(using
          any[Decoder[DiagnosticModeResponse]](),
          any[Encoder[DiagnosticModeResponse]]()
        )
      ).thenReturn(Future.successful(diagnosticModeResponse))
      sequencer.diagnosticMode(startTime, hint).futureValue should ===(diagnosticModeResponse)
    }

    "call postClient with OperationsMode request | ESW-143, ESW-101, ESW-362" in {
      val operationsModeResponse = mock[OperationsModeResponse]
      when(
        postClient.requestResponse[OperationsModeResponse](argsEq(OperationsMode))(using
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
          .requestResponse[SubmitResponse](argsEq(QueryFinal(id, timeout)), any[FiniteDuration]())(using
            any[Decoder[SubmitResponse]](),
            any[Encoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(submitResponse))
      sequencer.queryFinal(id).futureValue should ===(submitResponse)
    }

    "call postClient with GetSequenceComponent request | ESW-222, ESW-255" in {
      val sequenceComponentLocation =
        PekkoLocation(
          PekkoConnection(ComponentId(Prefix(randomSubsystem, "primary"), SequenceComponent)),
          new URI("mock-uri"),
          Metadata.empty
        )

      when(
        postClient
          .requestResponse[PekkoLocation](argsEq(GetSequenceComponent))(using
            any[Decoder[PekkoLocation]](),
            any[Encoder[PekkoLocation]]()
          )
      ).thenReturn(Future.successful(sequenceComponentLocation))
      sequencer.getSequenceComponent.futureValue should ===(sequenceComponentLocation)
    }

    "call postClient with GetSequencerState request | ESW-482" in {
      val sequencerStateResponse = mock[SequencerState]
      when(
        postClient.requestResponse[SequencerState](argsEq(GetSequencerState))(using
          any[Decoder[SequencerState]](),
          any[Encoder[SequencerState]]()
        )
      ).thenReturn(Future.successful(sequencerStateResponse))

      sequencer.getSequencerState.futureValue should ===(sequencerStateResponse)
    }
  }
}
