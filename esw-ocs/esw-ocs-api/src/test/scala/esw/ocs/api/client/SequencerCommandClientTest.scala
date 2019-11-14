package esw.ocs.api.client

import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.api.protocol.{SequencerPostRequest, _}
import io.bullet.borer.Decoder
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.{ExecutionContext, Future}

class SequencerCommandClientTest extends BaseTestSuite with SequencerHttpCodecs {

  private val postClient                    = mock[Transport[SequencerPostRequest]]
  private val websocketClient               = mock[Transport[SequencerWebsocketRequest]]
  private implicit val ec: ExecutionContext = mock[ExecutionContext]
  private val sequencerCommandClient        = new SequencerCommandClient(postClient, websocketClient)
  "SequencerCommandClient" must {

    "call postClient with GoOffline request | ESW-222" in {
      when(postClient.requestResponse[OkOrUnhandledResponse](argsEq(GoOffline))(any[Decoder[OkOrUnhandledResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerCommandClient.goOffline().futureValue should ===(Ok)
    }

    "call postClient with GoOnline request | ESW-222" in {
      when(postClient.requestResponse[GoOnlineResponse](argsEq(GoOnline))(any[Decoder[GoOnlineResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerCommandClient.goOnline().futureValue should ===(Ok)
    }

    "call postClient with LoadSequence request | ESW-222" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      when(
        postClient.requestResponse[OkOrUnhandledResponse](argsEq(LoadSequence(sequence)))(any[Decoder[OkOrUnhandledResponse]]())
      ).thenReturn(Future.successful(Ok))
      sequencerCommandClient.loadSequence(sequence).futureValue should ===(Ok)
    }

    "call postClient with StartSequence request | ESW-222" in {
      val startedResponse = Started(Id("runId"))
      when(postClient.requestResponse[SubmitResponse](argsEq(StartSequence))(any[Decoder[SubmitResponse]]()))
        .thenReturn(Future.successful(startedResponse))
      sequencerCommandClient.startSequence().futureValue should ===(startedResponse)
    }

    "call postClient with LoadAndStartSequence request | ESW-222" in {
      val command1         = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence         = Sequence(command1)
      val sequenceResponse = Started(sequence.runId)
      when(
        postClient
          .requestResponse[SubmitResponse](argsEq(SubmitSequence(sequence)))(any[Decoder[SubmitResponse]]())
      ).thenReturn(Future.successful(sequenceResponse))
      sequencerCommandClient.submit(sequence).futureValue should ===(sequenceResponse)
    }

    "call postClient with DiagnosticMode request | ESW-143" in {
      val startTime = UTCTime.now()
      val hint      = "engineering"
      when(
        postClient.requestResponse[DiagnosticModeResponse](argsEq(DiagnosticMode(startTime, hint)))(
          any[Decoder[DiagnosticModeResponse]]()
        )
      ).thenReturn(Future.successful(Ok))
      sequencerCommandClient.diagnosticMode(startTime, hint).futureValue should ===(Ok)
    }

    "call postClient with OperationsMode request | ESW-143" in {
      when(postClient.requestResponse[OperationsModeResponse](argsEq(OperationsMode))(any[Decoder[OperationsModeResponse]]()))
        .thenReturn(Future.successful(Ok))
      sequencerCommandClient.operationsMode().futureValue should ===(Ok)
    }

    "call websocket with QueryFinal request | ESW-222" in {
      val id = mock[Id]
      when(websocketClient.requestResponse[SubmitResponse](argsEq(QueryFinal))(any[Decoder[SubmitResponse]]()))
        .thenReturn(Future.successful(Completed(id)))
      sequencerCommandClient.queryFinal().futureValue should ===(Completed(id))
    }
  }
}
