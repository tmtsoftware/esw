package esw.ocs.api.client

import akka.NotUsed
import akka.stream.scaladsl.Source
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.{SequencerInsight, StepList}
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import esw.ocs.api.protocol.SequencerAdminWebsocketRequest.{GetInsights, QueryFinal}
import esw.ocs.api.protocol.{SequencerAdminWebsocketRequest, _}
import msocket.api.Transport
import scala.concurrent.Future

class SequencerAdminClient(
    postClient: Transport[SequencerAdminPostRequest],
    websocketClient: Transport[SequencerAdminWebsocketRequest]
) extends SequencerAdminApi
    with SequencerAdminHttpCodecs {

  override def getSequence: Future[Option[StepList]] = {
    postClient.requestResponse[Option[StepList]](GetSequence)
  }

  override def isAvailable: Future[Boolean] = {
    postClient.requestResponse[Boolean](IsAvailable)
  }

  override def isOnline: Future[Boolean] = {
    postClient.requestResponse[Boolean](IsOnline)
  }

  override def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](Add(commands))
  }

  override def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](Prepend(commands))
  }

  override def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = {
    postClient.requestResponse[GenericResponse](Replace(id, commands))
  }

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = {
    postClient.requestResponse[GenericResponse](InsertAfter(id, commands))
  }

  override def delete(id: Id): Future[GenericResponse] = {
    postClient.requestResponse[GenericResponse](Delete(id))
  }

  override def pause: Future[PauseResponse] = {
    postClient.requestResponse[PauseResponse](Pause)
  }

  override def resume: Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](Resume)
  }

  override def addBreakpoint(id: Id): Future[GenericResponse] = {
    postClient.requestResponse[GenericResponse](AddBreakpoint(id))
  }

  override def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] = {
    postClient.requestResponse[RemoveBreakpointResponse](RemoveBreakpoint(id))
  }

  override def reset(): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](Reset)
  }

  override def abortSequence(): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](AbortSequence)
  }

  override def stop(): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](Stop)
  }

  override def goOnline(): Future[GoOnlineResponse] = {
    postClient.requestResponse[GoOnlineResponse](GoOnline)
  }

  override def goOffline(): Future[GoOfflineResponse] = {
    postClient.requestResponse[GoOfflineResponse](GoOffline)
  }

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] = {
    postClient.requestResponse[DiagnosticModeResponse](DiagnosticMode(startTime, hint))
  }

  override def operationsMode(): Future[OperationsModeResponse] = {
    postClient.requestResponse[OperationsModeResponse](OperationsMode)
  }

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](LoadSequence(sequence))
  }

  override def startSequence: Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](StartSequence)
  }

  override def submitSequence(sequence: Sequence): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](SubmitSequence(sequence))
  }

  override def queryFinal: Future[SequenceResponse] = {
    websocketClient.requestResponse[SequenceResponse](QueryFinal)
  }

  override def getInsights: Source[SequencerInsight, NotUsed] =
    websocketClient.requestStream[SequencerInsight](GetInsights)
}
