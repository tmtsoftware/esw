package esw.ocs.api.client

import akka.util.Timeout
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
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
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong

class SequencerAdminClientTest extends BaseTestSuite with SequencerHttpCodecs {

  private val postClient           = mock[Transport[SequencerPostRequest]]
  private val websocketClient      = mock[Transport[SequencerWebsocketRequest]]
  private val sequencerAdminClient = new SequencerAdminClient(postClient, websocketClient)

  "SequencerAdminClient" must {

    "call postClient with GetSequence request | ESW-222" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      val stepList = StepList(sequence)

      when(postClient.requestResponse[Option[StepList]](argsEq(GetSequence))(any[Decoder[Option[StepList]]]()))
        .thenReturn(Future.successful(Some(stepList)))
      sequencerAdminClient.getSequence.futureValue.get should ===(stepList)
    }

    "call postClient with IsAvailable request | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsAvailable))(any[Decoder[Boolean]]())).thenReturn(Future.successful(true))
      sequencerAdminClient.isAvailable.futureValue should ===(true)
    }

    "call postClient with IsOnline request | ESW-222" in {
      when(postClient.requestResponse[Boolean](argsEq(IsOnline))(any[Decoder[Boolean]]())).thenReturn(Future.successful(true))
      sequencerAdminClient.isOnline.futureValue should ===(true)
    }

    "call postClient with Add request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Add(List.empty)))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.add(List.empty).futureValue should ===(Ok)
    }

    "call postClient with Prepend request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Prepend(List.empty)))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.prepend(List.empty).futureValue should ===(Ok)
    }

    "call postClient with Replace request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(Replace(id, List.empty)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.replace(id, List.empty).futureValue should ===(Ok)
    }

    "call postClient with InsertAfter request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(InsertAfter(id, List.empty)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.insertAfter(id, List.empty).futureValue should ===(Ok)
    }

    "call postClient with Delete request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(Delete(id)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.delete(id).futureValue should ===(Ok)
    }

    "call postClient with Pause request | ESW-222" in {
      when(postClient.requestResponse[PauseResponse](argsEq(Pause))(any[Decoder[PauseResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.pause.futureValue should ===(Ok)
    }

    "call postClient with Resume request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Resume))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.resume.futureValue should ===(Ok)
    }

    "call postClient with AddBreakpoint request | ESW-222" in {
      val id = mock[Id]
      when(postClient.requestResponse[GenericResponse](argsEq(AddBreakpoint(id)))(any[Decoder[GenericResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.addBreakpoint(id).futureValue should ===(Ok)
    }

    "call postClient with RemoveBreakpoint request | ESW-222" in {
      val id = mock[Id]
      when(
        postClient
          .requestResponse[RemoveBreakpointResponse](argsEq(RemoveBreakpoint(id)))(any[Decoder[RemoveBreakpointResponse]]())
      ).thenReturn(Future.successful(Ok))
      sequencerAdminClient.removeBreakpoint(id).futureValue should ===(Ok)
    }

    "call postClient with Reset request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Reset))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.reset().futureValue should ===(Ok)
    }

    "call postClient with AbortSequence request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(AbortSequence))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.abortSequence().futureValue should ===(Ok)
    }

    "call postClient with Stop request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(Stop))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.stop().futureValue should ===(Ok)
    }

    "call postClient with GoOffline request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(GoOffline))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.goOffline().futureValue should ===(Ok)
    }

    "call postClient with GoOnline request | ESW-222" in {
      when(postClient.requestResponse[GoOnlineResponse](argsEq(GoOnline))(any[Decoder[GoOnlineResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.goOnline().futureValue should ===(Ok)
    }

    "call postClient with LoadSequence request | ESW-222" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(LoadSequence(sequence)))(any[Decoder[OkOrUnhandledResponse]]())
      ).thenReturn(Future.successful(Ok))
      sequencerAdminClient.loadSequence(sequence).futureValue should ===(Ok)
    }

    "call postClient with StartSequence request | ESW-222" in {
      val startedResponse = Started(Id("runId"))
      when(postClient.requestResponse[SubmitResponse](argsEq(StartSequence))(any[Decoder[SubmitResponse]]()))
        .thenReturn(Future.successful(startedResponse))
      sequencerAdminClient.startSequence().futureValue should ===(startedResponse)
    }

    "call postClient with SubmitSequence request | ESW-222" in {
      val command1         = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence         = Sequence(command1)
      val sequenceId       = Id()
      val sequenceResponse = Started(sequenceId)
      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(SubmitSequence(sequence)))(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(sequenceResponse))
      sequencerAdminClient.submit(sequence).futureValue should ===(sequenceResponse)
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
          .requestResponse[SubmitResponse](argsEq(SubmitSequence(sequence)))(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(startedResponse))

      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(sequenceId, timeout)), any[FiniteDuration]())(
            any[Decoder[SubmitResponse]]()
          )
      ).thenReturn(Future.successful(completedResponse))

      sequencerAdminClient.submitAndWait(sequence).futureValue should ===(completedResponse)
    }

    "call postClient with DiagnosticMode request | ESW-143" in {
      val startTime = UTCTime.now()
      val hint      = "engineering"
      when(
        postClient.requestResponse[DiagnosticModeResponse](argsEq(DiagnosticMode(startTime, hint)))(
          any[Decoder[DiagnosticModeResponse]]()
        )
      ).thenReturn(Future.successful(Ok))
      sequencerAdminClient.diagnosticMode(startTime, hint).futureValue should ===(Ok)
    }

    "call postClient with OperationsMode request | ESW-143" in {
      when(postClient.requestResponse[OperationsModeResponse](argsEq(OperationsMode))(any[Decoder[OperationsModeResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerAdminClient.operationsMode().futureValue should ===(Ok)
    }

    "call websocket with QueryFinal request | ESW-222" in {
      val id = mock[Id]

      implicit val timeout: Timeout = Timeout(10.seconds)
      when(
        websocketClient
          .requestResponse[SubmitResponse](argsEq(QueryFinal(id, timeout)), any[FiniteDuration]())(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(Completed(id)))
      sequencerAdminClient.queryFinal(id).futureValue should ===(Completed(id))
    }
  }
}
