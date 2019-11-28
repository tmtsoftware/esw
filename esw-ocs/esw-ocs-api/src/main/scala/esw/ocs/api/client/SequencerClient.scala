package esw.ocs.api.client

import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandServiceExtension
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.commands.{Sequence, SequenceCommand}
import akka.stream.scaladsl.Source
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.models.{SequencerInsight, StepList}
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol.SequencerWebsocketRequest._
import esw.ocs.api.protocol._
import msocket.api.Transport
import msocket.api.models.Subscription

import scala.concurrent.{ExecutionContext, Future}

class SequencerClient(
    postClient: Transport[SequencerPostRequest],
    websocketClient: Transport[SequencerWebsocketRequest]
)(implicit ec: ExecutionContext)
    extends SequencerApi
    with SequencerHttpCodecs {

  private val extensions = new SequencerCommandServiceExtension(this)

  override def getSequence: Future[Option[StepList]] = postClient.requestResponse[Option[StepList]](GetSequence)

  override def isAvailable: Future[Boolean] = postClient.requestResponse[Boolean](IsAvailable)

  override def isOnline: Future[Boolean] = postClient.requestResponse[Boolean](IsOnline)

  override def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](Add(commands))

  override def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](Prepend(commands))

  override def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] =
    postClient.requestResponse[GenericResponse](Replace(id, commands))

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] =
    postClient.requestResponse[GenericResponse](InsertAfter(id, commands))

  override def delete(id: Id): Future[GenericResponse] = postClient.requestResponse[GenericResponse](Delete(id))

  override def pause: Future[PauseResponse] = {
    postClient.requestResponse[PauseResponse](Pause)
  }

  override def resume: Future[OkOrUnhandledResponse] = postClient.requestResponse[OkOrUnhandledResponse](Resume)

  override def addBreakpoint(id: Id): Future[GenericResponse] =
    postClient.requestResponse[GenericResponse](AddBreakpoint(id))

  override def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] =
    postClient.requestResponse[RemoveBreakpointResponse](RemoveBreakpoint(id))

  override def reset(): Future[OkOrUnhandledResponse] = postClient.requestResponse[OkOrUnhandledResponse](Reset)

  override def abortSequence(): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](AbortSequence)

  override def stop(): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](Stop)

  override def getInsights: Source[SequencerInsight, Subscription] = websocketClient.requestStream[SequencerInsight](GetInsights)

  // commandApi
  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](LoadSequence(sequence))

  override def startSequence(): Future[SubmitResponse] = postClient.requestResponse[SubmitResponse](StartSequence)

  override def submit(sequence: Sequence): Future[SubmitResponse] =
    postClient.requestResponse[SubmitResponse](Submit(sequence))

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] =
    extensions.submitAndWait(sequence)

  override def query(runId: Id): Future[QueryResponse] =
    postClient.requestResponse[QueryResponse](Query(runId))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    websocketClient.requestResponse[SubmitResponse](QueryFinal(runId, timeout), timeout.duration)

  override def goOnline(): Future[GoOnlineResponse] = postClient.requestResponse[GoOnlineResponse](GoOnline)

  override def goOffline(): Future[GoOfflineResponse] = postClient.requestResponse[GoOfflineResponse](GoOffline)

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    postClient.requestResponse[DiagnosticModeResponse](DiagnosticMode(startTime, hint))

  override def operationsMode(): Future[OperationsModeResponse] =
    postClient.requestResponse[OperationsModeResponse](OperationsMode)
}
