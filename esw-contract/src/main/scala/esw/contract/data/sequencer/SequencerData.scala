package esw.contract.data.sequencer

import java.net.URI
import java.time.Instant

import csw.contract.data.command.CommandData
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata}
import csw.params.commands._
import csw.time.core.models.UTCTime
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.api.protocol._

trait SequencerData extends CommandData {
  val observeSequenceCommand: Observe = Observe(prefix, commandName, Some(obsId))
  val setupSequenceCommand: Setup     = Setup(prefix, commandName, Some(obsId))
  val waitSequenceCommand: Wait       = Wait(prefix, commandName, Some(obsId))

  val sequence: Sequence = Sequence(setupSequenceCommand, observeSequenceCommand, waitSequenceCommand)

  val ok: Ok                         = Ok
  val unhandled: Unhandled           = Unhandled("offline", "StartSequence")
  val idDoesNotExist: IdDoesNotExist = IdDoesNotExist(id)
  val cannotOperateOnAnInFlightOrFinishedStep: CannotOperateOnAnInFlightOrFinishedStep.type =
    CannotOperateOnAnInFlightOrFinishedStep
  val stepList: StepList                              = StepList(sequence)
  val step: Step                                      = Step(setupSequenceCommand)
  val goOnlineHookFailed: GoOnlineHookFailed.type     = GoOnlineHookFailed
  val goOfflineHookFailed: GoOfflineHookFailed.type   = GoOfflineHookFailed
  val diagnosticHookFailed: DiagnosticHookFailed.type = DiagnosticHookFailed
  val operationsHookFailed: OperationsHookFailed.type = OperationsHookFailed
  val akkaConnection: AkkaConnection                  = AkkaConnection(ComponentId(prefix, ComponentType.Assembly))
  val akkaLocation: AkkaLocation                      = AkkaLocation(akkaConnection, new URI("path"), Metadata(Map("key1" -> "value1")))

  val loadSequence: LoadSequence                      = LoadSequence(sequence)
  val startSequence: StartSequence.type               = StartSequence
  val add: Add                                        = Add(List(setupSequenceCommand, waitSequenceCommand, observeSequenceCommand))
  val prepend: Prepend                                = Prepend(List(waitSequenceCommand))
  val replace: Replace                                = Replace(id, List(setupSequenceCommand, observeSequenceCommand))
  val insertAfter: InsertAfter                        = InsertAfter(id, List(setupSequenceCommand))
  val delete: Delete                                  = Delete(id)
  val pause: Pause.type                               = Pause
  val resume: Resume.type                             = Resume
  val addBreakPoint: AddBreakpoint                    = AddBreakpoint(id)
  val removeBreakPoint: RemoveBreakpoint              = RemoveBreakpoint(id)
  val reset: Reset.type                               = Reset
  val abortSequence: AbortSequence.type               = AbortSequence
  val stop: Stop.type                                 = Stop
  val submit: Submit                                  = Submit(sequence)
  val query: Query                                    = Query(id)
  val goOnline: GoOnline.type                         = GoOnline
  val goOffline: GoOffline.type                       = GoOffline
  val diagnosticMode: DiagnosticMode                  = DiagnosticMode(UTCTime(Instant.ofEpochMilli(1000L)), "hint")
  val operationsMode: OperationsMode.type             = OperationsMode
  val getSequenceComponent: GetSequenceComponent.type = GetSequenceComponent
  val sequencerQueryFinal: QueryFinal                 = QueryFinal(id, timeout)
  val pendingStepStatus: StepStatus                   = StepStatus.Pending
  val inFlightStepStatus: StepStatus                  = StepStatus.InFlight
  val successStepStatus: StepStatus                   = StepStatus.Finished.Success
  val failureStepStatus: StepStatus                   = StepStatus.Finished.Failure("message")

}
