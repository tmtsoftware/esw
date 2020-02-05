package esw.contract.data.sequencer

import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.api.protocol._
import io.bullet.borer.Encoder

object SequencerContract extends SequencerData with SequencerHttpCodecs {
  val models: ModelSet = ModelSet(
    ModelType[SequenceCommand](observeSequenceCommand, setupSequenceCommand, waitSequenceCommand),
    ModelType[OkOrUnhandledResponse](ok, unhandled),
    ModelType[SubmitResponse](completed, cancelled, invalid, error, locked, started),
    ModelType[GenericResponse](ok, unhandled, idDoesNotExist, cannotOperateOnAnInFlightOrFinishedStep),
    ModelType[PauseResponse](ok, unhandled, cannotOperateOnAnInFlightOrFinishedStep),
    ModelType[RemoveBreakpointResponse](ok, unhandled, idDoesNotExist),
    ModelType[GoOnlineResponse](ok, unhandled, goOnlineHookFailed),
    ModelType[GoOfflineResponse](ok, unhandled, goOfflineHookFailed),
    ModelType[DiagnosticModeResponse](ok, diagnosticHookFailed),
    ModelType[OperationsModeResponse](ok, operationsHookFailed),
    ModelType(akkaLocation)
  )

  implicit def httpEnc[Sub <: SequencerPostRequest]: Encoder[Sub]           = SubTypeCodec.encoder(sequencerPostRequestValue)
  implicit def websocketEnc[Sub <: SequencerWebsocketRequest]: Encoder[Sub] = SubTypeCodec.encoder(sequencerWebsocketRequestValue)

  val httpRequests: ModelSet = ModelSet(
    ModelType(loadSequence),
    ModelType(add),
    ModelType(prepend),
    ModelType(replace),
    ModelType(insertAfter),
    ModelType(delete),
    ModelType(pause),
    ModelType(resume),
    ModelType(addBreakPoint),
    ModelType(removeBreakPoint),
    ModelType(reset),
    ModelType(abortSequence),
    ModelType(stop),
    ModelType(submit),
    ModelType(query),
    ModelType(goOnline),
    ModelType(goOffline),
    ModelType(diagnosticMode),
    ModelType(operationsMode),
    ModelType(getSequenceComponent)
  )

  val websocketRequests: ModelSet = ModelSet(
    ModelType(sequencerQueryFinal)
  )

  val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[LoadSequence], name[OkOrUnhandledResponse]),
    Endpoint(objectName(StartSequence), name[SubmitResponse]),
    Endpoint(objectName(GetSequence), arrayName[StepList]),
    Endpoint(name[Add], name[OkOrUnhandledResponse]),
    Endpoint(name[Prepend], name[OkOrUnhandledResponse]),
    Endpoint(name[Replace], name[OkOrUnhandledResponse]),
    Endpoint(name[InsertAfter], name[OkOrUnhandledResponse]),
    Endpoint(name[Delete], name[GenericResponse]),
    Endpoint(objectName(Pause), name[PauseResponse]),
    Endpoint(objectName(Resume), name[OkOrUnhandledResponse]),
    Endpoint(name[AddBreakpoint], name[GenericResponse]),
    Endpoint(name[RemoveBreakpoint], name[RemoveBreakpointResponse]),
    Endpoint(objectName(Reset), name[OkOrUnhandledResponse]),
    Endpoint(objectName(AbortSequence), name[OkOrUnhandledResponse]),
    Endpoint(objectName(Stop), name[OkOrUnhandledResponse]),
    Endpoint(name[Submit], name[SubmitResponse]),
    Endpoint(name[Query], name[SubmitResponse]),
    Endpoint(objectName(GoOnline), name[GoOnlineResponse]),
    Endpoint(objectName(GoOffline), name[GoOfflineResponse]),
    Endpoint(name[DiagnosticMode], name[DiagnosticModeResponse]),
    Endpoint(objectName(OperationsMode), name[OperationsModeResponse]),
    Endpoint(objectName(GetSequenceComponent), name[AkkaLocation])
  )

  val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[QueryFinal], name[SubmitResponse])
  )

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract(webSocketEndpoints, websocketRequests),
    models = models
  )
}
