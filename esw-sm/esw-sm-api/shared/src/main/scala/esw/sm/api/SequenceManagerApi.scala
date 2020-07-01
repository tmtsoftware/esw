package esw.sm.api

import csw.prefix.models.Subsystem
import esw.ocs.api.models.ObsMode
import esw.sm.api.protocol._

import scala.concurrent.Future

trait SequenceManagerApi {
  def configure(observingMode: ObsMode): Future[ConfigureResponse]
  def cleanup(observingMode: ObsMode): Future[CleanupResponse]
  def getRunningObsModes: Future[GetRunningObsModesResponse]
  def startSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[StartSequencerResponse]
  def shutdownSequencer(
      subsystem: Subsystem,
      observingMode: ObsMode
  ): Future[ShutdownSequencerResponse]
  def restartSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[RestartSequencerResponse]
  def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse]
  def shutdownSequenceComponent(subsystem: Subsystem, componentName: String): Future[ShutdownSequenceComponentResponse]
  def spawnSequenceComponent(
      machineSubsystem: Subsystem,
      machineName: String,
      seqCompName: String
  ): Future[SpawnSequenceComponentResponse]
}
