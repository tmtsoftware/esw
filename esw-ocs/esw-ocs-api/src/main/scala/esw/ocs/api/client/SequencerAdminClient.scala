package esw.ocs.api.client

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.request.SequencerAdminPostRequest
import esw.ocs.api.request.SequencerAdminPostRequest._
import esw.ocs.api.responses.{GenericResponse, GoOnlineResponse, OkOrUnhandledResponse, PauseResponse, RemoveBreakpointResponse}
import msocket.api.RequestClient

import scala.concurrent.Future

class SequencerAdminClient(postClient: RequestClient[SequencerAdminPostRequest])
    extends SequencerAdminApi
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

  override def goOnline(): Future[GoOnlineResponse] = {
    postClient.requestResponse[GoOnlineResponse](GoOnline)
  }

  override def goOffline(): Future[OkOrUnhandledResponse] = {
    postClient.requestResponse[OkOrUnhandledResponse](GoOffline)
  }
}
