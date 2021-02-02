package esw.sm.api

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol._

import scala.concurrent.Future

/**
 * A Sequence Manager API for interacting with Sequence Manager component.
 */
trait SequenceManagerApi {

  /**
   * Configures sequencers and resources needed for provided observing mode.
   * @param obsMode observing mode for configuration
   * @return a future of [[esw.sm.api.protocol.ConfigureResponse]] which completes with Success or Failure response ADT.
   */
  def configure(obsMode: ObsMode): Future[ConfigureResponse]

  /**
   * Spawns specified number of sequence components on specified agents.
   * @param config provision config which specifies number of sequence components needed to be provisioned on specific agents
   * @return a future of [[esw.sm.api.protocol.ProvisionResponse]] which completes with Success or Failure response ADT.
   */
  def provision(config: ProvisionConfig): Future[ProvisionResponse]

  /**
   * Returns all running observing modes
   * @return a future of [[esw.sm.api.protocol.GetRunningObsModesResponse]] which completes with Success or Failure response ADT.
   *         Success response gives information of all running observing modes.
   */
  def getRunningObsModes: Future[GetRunningObsModesResponse]

  /**
   * Returns all observing modes with their status
   *
   * @return a future of [[esw.sm.api.protocol.ObsModesWithStatusResponse]] which completes with Success or Failure response ADT.
   *         Success response gives information of all observing modes with their status.
   */
  def getObsModesWithStatus: Future[ObsModesWithStatusResponse]

  /**
   * Starts sequencer of provided Subsystem and Observing mode.
   * @param subsystem for sequencer needs to be started
   * @param obsMode for sequencer needs to be started
   * @return a future of [[esw.sm.api.protocol.StartSequencerResponse]] which completes with Success or Failure response ADT.
   */
  def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse]

  /**
   * Restarts running sequencer of provided Subsystem and Observing mode.
   * @param subsystem for sequencer needs to be re-started
   * @param obsMode for sequencer needs to be re-started
   * @return a future of [[esw.sm.api.protocol.RestartSequencerResponse]] which completes with Success or Failure response ADT.
   */
  def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse]

  /**
   * Shutdown running sequencer of provided Subsystem and Observing mode.
   * @param subsystem of sequencer needs to be shutdown
   * @param obsMode of sequencer needs to be shutdown
   * @return a future of [[esw.sm.api.protocol.ShutdownSequencersResponse]] which completes with Success or Failure response ADT.
   */
  def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[ShutdownSequencersResponse]

  /**
   * Shutdown all running sequencers of provided Subsystem
   * @param subsystem of sequencers needs to be shutdown
   * @return a future of [[esw.sm.api.protocol.ShutdownSequencersResponse]] which completes with Success or Failure response ADT.
   */
  def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse]

  /**
   * Shutdown all running sequencers of provided Observing mode
   * @param obsMode of sequencers needs to be shutdown
   * @return a future of [[esw.sm.api.protocol.ShutdownSequencersResponse]] which completes with Success or Failure response ADT.
   */
  def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse]

  /**
   * Shutdown all running sequencers
   * @return a future of [[esw.sm.api.protocol.ShutdownSequencersResponse]] which completes with Success or Failure response ADT.
   */
  def shutdownAllSequencers(): Future[ShutdownSequencersResponse]

  /**
   * Shutdown sequence component of provided prefix. This shuts down sequence component as well as sequencer running on sequence component (if any)
   * @param prefix of sequence component needs to be shutdown
   * @return a future of [[esw.sm.api.protocol.ShutdownSequenceComponentResponse]] which completes with Success or Failure response ADT
   */
  def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse]

  /**
   * Shutdown all running sequence components
   * @return a future of [[esw.sm.api.protocol.ShutdownSequenceComponentResponse]] which completes with Success or Failure response ADT
   */
  def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse]

  /**
   * Gives information of all running agents, sequence components running on those agents as well as orphan sequence
   * components (agent unknown) and sequencers running on sequence components.
   * @return a future of [[esw.sm.api.protocol.AgentStatusResponse]] which completes with Success or Failure response ADT
   */
  def getAgentStatus: Future[AgentStatusResponse]

  /**
   * Gives information of all resources which are being used by obsMode in given sequence manager config file
   * each resource will have its status [[esw.sm.api.models.ResourceStatus]] and obsMode if it is in use
   * @return a future of [[esw.sm.api.protocol.ResourcesStatusResponse]] which completes with Success or Failure response ADT
   */
  def getResources: Future[ResourcesStatusResponse]
}
