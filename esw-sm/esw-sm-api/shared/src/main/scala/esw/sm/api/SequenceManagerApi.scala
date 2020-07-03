package esw.sm.api

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.protocol._

import scala.concurrent.Future

trait SequenceManagerApi {
  def configure(observingMode: ObsMode): Future[ConfigureResponse]
  def getRunningObsModes: Future[GetRunningObsModesResponse]

  def startSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[StartSequencerResponse]
  def restartSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[RestartSequencerResponse]

  def shutdownSequencer(subsystem: Subsystem, observingMode: ObsMode): Future[ShutdownSequencersResponse]
  def shutdownObsModeSequencers(observingMode: ObsMode): Future[ShutdownSequencersResponse]
  def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse]
  def shutdownAllSequencers(): Future[ShutdownSequencersResponse]
  private[sm] def shutdownSequencers(shutdownSequencersPolicy: ShutdownSequencersPolicy): Future[ShutdownSequencersResponse]

  def spawnSequenceComponent(agent: Prefix, name: String): Future[SpawnSequenceComponentResponse]
  def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse]
}
