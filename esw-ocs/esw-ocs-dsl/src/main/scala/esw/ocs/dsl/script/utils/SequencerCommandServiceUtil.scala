package esw.ocs.dsl.script.utils

import java.util.concurrent.CompletionStage

import csw.command.api.scaladsl.SequencerCommandService
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.protocol._

import scala.compat.java8.FutureConverters.FutureOps

class SequencerCommandServiceUtil(sequencerCommandService: SequencerCommandService, sequencerAdmin: SequencerAdminApi) {

  def submitAndWait(sequence: Sequence): CompletionStage[SubmitResponse] = sequencerCommandService.submitAndWait(sequence).toJava
  def queryFinal(): CompletionStage[SubmitResponse]                      = sequencerCommandService.queryFinal().toJava

  def submit(sequence: Sequence): CompletionStage[OkOrUnhandledResponse] = sequencerAdmin.submitSequence(sequence).toJava

  def goOnline(): CompletionStage[GoOnlineResponse]   = sequencerAdmin.goOnline().toJava
  def goOffline(): CompletionStage[GoOfflineResponse] = sequencerAdmin.goOffline().toJava

  def diagnosticMode(startTime: UTCTime, hint: String): CompletionStage[DiagnosticModeResponse] =
    sequencerAdmin.diagnosticMode(startTime, hint).toJava
  def operationsMode(): CompletionStage[OperationsModeResponse] = sequencerAdmin.operationsMode().toJava
  def abortSequence(): CompletionStage[OkOrUnhandledResponse]   = sequencerAdmin.abortSequence().toJava

}
