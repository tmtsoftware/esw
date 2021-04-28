package esw.contract.data.sequencer

import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol.SequencerRequest._
import esw.ocs.api.protocol.ExternalSequencerState._
import esw.ocs.api.protocol.SequencerStreamRequest.QueryFinal
import esw.ocs.api.protocol._

// ESW-278 Contract samples for sequencer service. These samples are also used in `RoundTripTest`
object SequencerContract extends SequencerData with SequencerServiceCodecs {
  private val models: ModelSet = ModelSet.models(
    ModelType[SequenceCommand](
      observeSequenceCommand,
      setupSequenceCommand,
      waitSequenceCommand,
      waitSequenceCommandWithoutObsId
    ),
    ModelType[OkOrUnhandledResponse](ok, unhandled),
    ModelType[SubmitResponse](completed, cancelled, invalid, error, locked, started),
    ModelType[GenericResponse](ok, unhandled, idDoesNotExist, cannotOperateOnAnInFlightOrFinishedStep),
    ModelType[PauseResponse](ok, unhandled, cannotOperateOnAnInFlightOrFinishedStep),
    ModelType[RemoveBreakpointResponse](ok, unhandled, idDoesNotExist),
    ModelType[GoOnlineResponse](ok, unhandled, goOnlineHookFailed),
    ModelType[GoOfflineResponse](ok, unhandled, goOfflineHookFailed),
    ModelType[DiagnosticModeResponse](ok, diagnosticHookFailed),
    ModelType[OperationsModeResponse](ok, operationsHookFailed),
    ModelType(akkaLocation),
    ModelType[StepList](stepList),
    ModelType[Step](step),
    ModelType[StepStatus](pendingStepStatus, inFlightStepStatus, successStepStatus, failureStepStatus),
    ModelType[ExternalSequencerState](Idle, Running, Offline, Loaded, Processing)
  )

  private val httpRequests = new RequestSet[SequencerRequest] {
    requestType(loadSequence)
    requestType(add)
    requestType(prepend)
    requestType(replace)
    requestType(insertAfter)
    requestType(delete)
    requestType(pause)
    requestType(resume)
    requestType(addBreakPoint)
    requestType(removeBreakPoint)
    requestType(reset)
    requestType(abortSequence)
    requestType(stop)
    requestType(submit)
    requestType(query)
    requestType(goOnline)
    requestType(goOffline)
    requestType(diagnosticMode)
    requestType(operationsMode)
    requestType(getSequenceComponent)
    requestType(getSequencerState)
  }

  private val websocketRequests = new RequestSet[SequencerStreamRequest] {
    requestType(sequencerQueryFinal)
  }

  private val httpEndpoints: List[Endpoint] = List(
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
    Endpoint(objectName(GetSequenceComponent), name[AkkaLocation]),
    Endpoint(objectName(GetSequencerState), name[ExternalSequencerState])
  )

  private val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[QueryFinal], name[SubmitResponse])
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("sequencer-service/README.md"))

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract(webSocketEndpoints, websocketRequests),
    models = models,
    readme = readme
  )
}
