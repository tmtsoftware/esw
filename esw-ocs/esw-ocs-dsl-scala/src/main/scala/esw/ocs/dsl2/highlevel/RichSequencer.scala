package esw.ocs.dsl2.highlevel

import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.api.models.Variation
import esw.ocs.api.protocol.*
import esw.ocs.dsl2.highlevel.models.CommandError
import esw.ocs.dsl2.Extensions.*

import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import async.Async.*
import scala.jdk.FutureConverters.CompletionStageOps

class RichSequencer(
    subsystem: Subsystem,
    obsMode: ObsMode,
    variation: Variation,
    sequencerApiFactory: (Subsystem, ObsMode, Variation) => CompletionStage[SequencerApi],
    defaultTimeout: Duration
)(using ExecutionContext) {

  private inline def sequencerService() = await(sequencerApiFactory(subsystem, obsMode, variation).asScala)

  inline def submit(sequence: Sequence, resumeOnError: Boolean = false): SubmitResponse =
    val submitResponse: SubmitResponse = await(sequencerService().submit(sequence))
    if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
    submitResponse

  inline def query(runId: Id, resumeOnError: Boolean = false): SubmitResponse =
    val submitResponse: SubmitResponse = await(sequencerService().query(runId))
    if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
    submitResponse

  inline def queryFinal(runId: Id, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse =
    val akkaTimeout                    = Timeout(timeout.length, timeout.unit)
    val submitResponse: SubmitResponse = await(sequencerService().queryFinal(runId)(using akkaTimeout))
    if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
    submitResponse

  inline def submitAndWait(
      sequence: Sequence,
      timeout: Duration = defaultTimeout,
      resumeOnError: Boolean = false
  ): SubmitResponse =
    val akkaTimeout                    = Timeout(timeout.length, timeout.unit)
    val submitResponse: SubmitResponse = await(sequencerService().submitAndWait(sequence)(using akkaTimeout))
    if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
    submitResponse

  inline def goOnline(): GoOnlineResponse = await(sequencerService().goOnline())

  inline def goOffline(): GoOfflineResponse = await(sequencerService().goOffline())

  inline def diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse = await(
    sequencerService().diagnosticMode(startTime, hint)
  )

  inline def operationsMode(): OperationsModeResponse = await(sequencerService().operationsMode())

  inline def abortSequence(): OkOrUnhandledResponse = await(sequencerService().abortSequence())

  inline def stop(): OkOrUnhandledResponse = await(sequencerService().stop())

}
