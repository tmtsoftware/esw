/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.client

import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.utils.SequencerCommandServiceExtension
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.models.{SequencerState, StepList}
import esw.ocs.api.protocol.*
import esw.ocs.api.protocol.SequencerRequest.*
import esw.ocs.api.protocol.SequencerStreamRequest.{QueryFinal, SubscribeSequencerState}
import msocket.api.{Subscription, Transport}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Http Client for the sequencer
 *
 * @param postClient -  a transport [[msocket.http.post.HttpPostTransport]] to communicate with http protocol
 * @param websocketClient - a transport [[msocket.http.ws.WebsocketTransport]] to communicate with websocket
 * @param ec - execution context
 */
class SequencerClient(
    postClient: Transport[SequencerRequest],
    websocketClient: Transport[SequencerStreamRequest]
)(implicit ec: ExecutionContext)
    extends SequencerApi
    with SequencerServiceCodecs {

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

  // commandApi
  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](LoadSequence(sequence))

  override def startSequence(): Future[SubmitResponse] = postClient.requestResponse[SubmitResponse](StartSequence)

  override def submit(sequence: Sequence): Future[SubmitResponse] =
    postClient.requestResponse[SubmitResponse](Submit(sequence))

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] =
    extensions.submitAndWait(sequence)

  override def query(runId: Id): Future[SubmitResponse] =
    postClient.requestResponse[SubmitResponse](Query(runId))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    websocketClient.requestResponse[SubmitResponse](QueryFinal(runId, timeout), timeout.duration)

  override def goOnline(): Future[GoOnlineResponse] = postClient.requestResponse[GoOnlineResponse](GoOnline)

  override def goOffline(): Future[GoOfflineResponse] = postClient.requestResponse[GoOfflineResponse](GoOffline)

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    postClient.requestResponse[DiagnosticModeResponse](DiagnosticMode(startTime, hint))

  override def operationsMode(): Future[OperationsModeResponse] =
    postClient.requestResponse[OperationsModeResponse](OperationsMode)

  override def getSequenceComponent: Future[AkkaLocation] = postClient.requestResponse[AkkaLocation](GetSequenceComponent)

  override def getSequencerState: Future[SequencerState] =
    postClient.requestResponse[SequencerState](GetSequencerState)

  override def subscribeSequencerState(): Source[SequencerStateResponse, Subscription] =
    websocketClient.requestStream[SequencerStateResponse](SubscribeSequencerState)

}
