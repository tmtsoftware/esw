package esw.sm.api

import csw.location.api.models.ComponentId
import csw.prefix.models.{Prefix, Subsystem}
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
  def spawnSequenceComponent(machine: ComponentId, name: String): Future[SpawnSequenceComponentResponse]
  def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse]
}
