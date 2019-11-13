package esw.ocs.dsl.script.utils

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import csw.command.client.SequencerCommandServiceFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.api.protocol._
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps

class SequencerCommandServiceUtil(
    sequencerAdminFactory: SequencerAdminFactoryApi,
    locationServiceUtil: LocationServiceUtil
)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext

  private def sequencerCommandService(sequencerId: String, observingMode: String) =
    locationServiceUtil.resolveSequencer(sequencerId, observingMode).map(SequencerCommandServiceFactory.make)
  private def sequencerAdmin(sequencerId: String, observingMode: String) = sequencerAdminFactory.make(sequencerId, observingMode)

  def submitAndWait(sequencerId: String, observingMode: String, sequence: Sequence): CompletionStage[SubmitResponse] =
    sequencerCommandService(sequencerId, observingMode).flatMap(_.submitAndWait(sequence)).toJava

  def goOnline(sequencerId: String, observingMode: String): CompletionStage[GoOnlineResponse] =
    sequencerAdmin(sequencerId, observingMode).flatMap(_.goOnline()).toJava

  def goOffline(sequencerId: String, observingMode: String): CompletionStage[GoOfflineResponse] =
    sequencerAdmin(sequencerId, observingMode).flatMap(_.goOffline()).toJava

  def diagnosticMode(
      sequencerId: String,
      observingMode: String,
      startTime: UTCTime,
      hint: String
  ): CompletionStage[DiagnosticModeResponse] =
    sequencerAdmin(sequencerId, observingMode).flatMap(_.diagnosticMode(startTime, hint)).toJava

  def operationsMode(sequencerId: String, observingMode: String): CompletionStage[OperationsModeResponse] =
    sequencerAdmin(sequencerId, observingMode).flatMap(_.operationsMode()).toJava

  def abortSequence(sequencerId: String, observingMode: String): CompletionStage[OkOrUnhandledResponse] =
    sequencerAdmin(sequencerId, observingMode).flatMap(_.abortSequence()).toJava

  def stop(sequencerId: String, observingMode: String): CompletionStage[OkOrUnhandledResponse] =
    sequencerAdmin(sequencerId, observingMode).flatMap(_.stop()).toJava

}
