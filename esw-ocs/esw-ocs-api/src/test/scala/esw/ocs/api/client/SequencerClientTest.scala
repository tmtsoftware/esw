package esw.ocs.api.client

import java.net.URI

import akka.util.Timeout
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Subsystem.ESW
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.api.protocol.{SequencerPostRequest, _}
import io.bullet.borer.Decoder
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class SequencerClientTest extends BaseTestSuite with SequencerHttpCodecs {

  private val postClient      = mock[Transport[SequencerPostRequest]]
  private val websocketClient = mock[Transport[SequencerWebsocketRequest]]
  private val sequencer       = new SequencerClient(postClient, websocketClient)

  "SequencerClient" must {

    "call postClient with GetSequence request | ESW-222" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      val stepList = StepList(sequence)

      when(postClient.requestResponse[Option[StepList]](argsEq(GetSequence))(any[Decoder[Option[StepList]]]()))
        .thenReturn(Future.successful(Some(stepList)))
      sequencer.getSequence.futureValue.get should ===(stepList)
    }

    "call postClient with IsAvailable request | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsAvailable))(any[Decoder[Boolean]]())).thenReturn(Future.successful(true))
      sequencer.isAvailable.futureValue should ===(true)
    }

    "call postClient with IsOnline request | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsOnline))(any[Decoder[Boolean]]())).thenReturn(Future.successful(true))
      sequencer.isOnline.futureValue should ===(true)
    }

    "call postClient with Add request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Add(List.empty)))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.add(List.empty).futureValue should ===(Ok)
    }

    "call postClient with Prepend request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Prepend(List.empty)))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.prepend(List.empty).futureValue should ===(Ok)
    }

    "call postClient with Replace request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(Replace(id, List.empty)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.replace(id, List.empty).futureValue should ===(Ok)
    }

    "call postClient with InsertAfter request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(InsertAfter(id, List.empty)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.insertAfter(id, List.empty).futureValue should ===(Ok)
    }

    "call postClient with Delete request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(Delete(id)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.delete(id).futureValue should ===(Ok)
    }

    "call postClient with Pause request | ESW-222" in {
      when(postClient.requestResponse[PauseResponse](argsEq(Pause))(any[Decoder[PauseResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.pause.futureValue should ===(Ok)
    }

    "call postClient with Resume request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Resume))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.resume.futureValue should ===(Ok)
    }

    "call postClient with AddBreakpoint request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(AddBreakpoint(id)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.addBreakpoint(id).futureValue should ===(Ok)
    }

    "call postClient with RemoveBreakpoint request | ESW-222" in {
      val id = mock[Id]
      when(
        postClient
          .requestResponse[RemoveBreakpointResponse](argsEq(RemoveBreakpoint(id)))(any[Decoder[RemoveBreakpointResponse]]())
      ).thenReturn(Future.successful(Ok))
      sequencer.removeBreakpoint(id).futureValue should ===(Ok)
    }

    "call postClient with Reset request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Reset))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.reset().futureValue should ===(Ok)
    }

    "call postClient with AbortSequence request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(AbortSequence))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.abortSequence().futureValue should ===(Ok)
    }

    "call postClient with Stop request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Stop))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.stop().futureValue should ===(Ok)
    }

    "call postClient with GoOffline request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(GoOffline))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.goOffline().futureValue should ===(Ok)
    }

    "call postClient with GoOnline request | ESW-222" in {
      when(postClient.requestResponse[GoOnlineResponse](argsEq(GoOnline))(any[Decoder[GoOnlineResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.goOnline().futureValue should ===(Ok)
    }

    "call postClient with LoadSequence request | ESW-222" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(LoadSequence(sequence)))(any[Decoder[OkOrUnhandledResponse]]())
      ).thenReturn(Future.successful(Ok))
      sequencer.loadSequence(sequence).futureValue should ===(Ok)
    }

    "call postClient with StartSequence request | ESW-222" in {
      val startedResponse = Started(Id("runId"))
      when(postClient.requestResponse[SubmitResponse](argsEq(StartSequence))(any[Decoder[SubmitResponse]]()))
        .thenReturn(Future.successful(startedResponse))
      sequencer.startSequence().futureValue should ===(startedResponse)
    }

    "call postClient with SubmitSequence request | ESW-222" in {
      val command1         = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence         = Sequence(command1)
      val sequenceId       = Id()
      val sequenceResponse = Started(sequenceId)
      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(Submit(sequence)))(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(sequenceResponse))
      sequencer.submit(sequence).futureValue should ===(sequenceResponse)
    }

    "call postClient with SubmitAndWait request | ESW-222" in {
      val command1                  = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence                  = Sequence(command1)
      val sequenceId                = Id()
      val startedResponse           = Started(sequenceId)
      val completedResponse         = Completed(sequenceId)
      implicit val timeout: Timeout = Timeout(10.seconds)

      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(Submit(sequence)))(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(startedResponse))

      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(sequenceId, timeout)), any[FiniteDuration]())(
            any[Decoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(completedResponse))

      sequencer.submitAndWait(sequence).futureValue should ===(completedResponse)
    }

    "call postClient with DiagnosticMode request | ESW-143" in {
      val startTime = UTCTime.now()
      val hint      = "engineering"
      when(
        postClient.requestResponse[DiagnosticModeResponse](argsEq(DiagnosticMode(startTime, hint)))(
          any[Decoder[DiagnosticModeResponse]]()
        )
      ).thenReturn(Future.successful(Ok))
      sequencer.diagnosticMode(startTime, hint).futureValue should ===(Ok)
    }

    "call postClient with OperationsMode request | ESW-143" in {
      when(postClient.requestResponse[OperationsModeResponse](argsEq(OperationsMode))(any[Decoder[OperationsModeResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencer.operationsMode().futureValue should ===(Ok)
    }

    "call websocket with QueryFinal request | ESW-222" in {
      val id = mock[Id]

      implicit val timeout: Timeout = Timeout(10.seconds)
      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(id, timeout)), any[FiniteDuration]())(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(Completed(id)))
      sequencer.queryFinal(id).futureValue should ===(Completed(id))
    }

    "call postClient with GetSequenceComponent request | ESW-222, ESW-255" in {
      val sequenceComponentLocation =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), SequenceComponent)), new URI("mock-uri"))

      when(postClient.requestResponse[AkkaLocation](argsEq(GetSequenceComponent))(any[Decoder[AkkaLocation]]()))
        .thenReturn(Future.successful(sequenceComponentLocation))
      sequencer.getSequenceComponent.futureValue should ===(sequenceComponentLocation)
    }
  }
}
