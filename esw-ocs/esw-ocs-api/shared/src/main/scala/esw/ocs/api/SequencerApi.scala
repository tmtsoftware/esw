package esw.ocs.api

import akka.stream.scaladsl.Source
import csw.command.api.scaladsl.SequencerCommandService
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.models.{SequencerState, StepList}
import esw.ocs.api.protocol.*
import msocket.api.Subscription

import scala.concurrent.Future

/**
 * Some extra APIs for the sequencer on top of the SequencerCommandService's apis
 */
trait SequencerApi extends SequencerCommandService {

  /**
   * Loads the given sequence to the sequencer.
   * If the sequencer is in Idle or Loaded state
   * then [[esw.ocs.api.protocol.Ok]] response is returned
   * otherwise [[esw.ocs.api.protocol.Unhandled]] response is returned
   *
   * @param sequence to run on the sequencer
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse]

  /**
   * Starts the loaded sequence in the sequencer.
   * If the sequencer is loaded
   * then a [[csw.params.commands.CommandResponse.Started]] response is returned
   * If the sequencer is already running another sequence, an [[csw.params.commands.CommandResponse.Invalid]] response is returned
   *
   * @return an initial [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future value
   */
  def startSequence(): Future[SubmitResponse]

  /**
   * Get the sequence in sequencer - current or last.
   * If there is no sequence
   * then None response is returned
   * otherwise [[esw.ocs.api.models.StepList]] is returned as Some value
   *
   * @return Option of [[esw.ocs.api.models.StepList]] as a Future value
   */
  def getSequence: Future[Option[StepList]]

  /**
   * Adds the given list of sequence commands at the end of the sequence
   *
   * @param commands - list of SequenceCommand to add in the sequence of sequencer
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]

  /**
   * Prepends the given list of sequence commands in the sequence
   *
   * @param commands - list of SequenceCommand to add in the sequence of sequencer
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]

  /**
   * Replaces the command of the given id with the given list of sequence commands in the sequence
   *
   * @param id - runId of command which is to be replaced
   * @param commands - list of SequenceCommand to replace with
   * @return a [[esw.ocs.api.protocol.GenericResponse]] as a Future value
   */
  def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]

  /**
   * Inserts the given list of sequence commands after the command of given id in the sequence
   *
   * @param id - runId of command after which the given list of commands is to be inserted
   * @param commands - list of SequenceCommand to be inserted
   * @return a [[esw.ocs.api.protocol.GenericResponse]] as a Future value
   */
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]

  /**
   * Deletes the command of the given id in the sequence
   *
   * @param id - runId of the command which is to be deleted
   * @return a [[esw.ocs.api.protocol.GenericResponse]] as a Future value
   */
  def delete(id: Id): Future[GenericResponse]

  /**
   * Adds a breakpoint at the command of the given id in the sequence
   *
   * @param id - runId of the command where breakpoint is to be added
   * @return a [[esw.ocs.api.protocol.GenericResponse]] as a Future value
   */
  def addBreakpoint(id: Id): Future[GenericResponse]

  /**
   * Removes a breakpoint from the command of the given id in the sequence
   *
   * @param id - runId of command where breakpoint is
   * @return a [[esw.ocs.api.protocol.GenericResponse]] as a Future value
   */
  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse]

  /**
   * Resets the sequence by discarding all the pending steps of the sequence
   *
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def reset(): Future[OkOrUnhandledResponse]

  /**
   * Pauses the running sequence
   *
   * @return an [[esw.ocs.api.protocol.PauseResponse]] as a Future value
   */
  def pause: Future[PauseResponse]

  /**
   * Resumes the paused sequence
   *
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def resume: Future[OkOrUnhandledResponse]

  /**
   * Return the akka location of sequence component where sequencer is running
   *
   * @return [[csw.location.api.models.AkkaLocation]] as a Future value
   */
  def getSequenceComponent: Future[AkkaLocation]

  /**
   * Checks if sequencer is in Idle state
   *
   * @return boolean as a Future value
   */
  def isAvailable: Future[Boolean]

  /**
   * Checks if sequencer is in Online(any state except Offline) state
   *
   * @return boolean as a Future value
   */
  def isOnline: Future[Boolean]

  /**
   * sends command to the sequencer to go in Online state if it is in Offline state
   *
   * @return a [[esw.ocs.api.protocol.GoOnlineResponse]] as a Future value
   */
  def goOnline(): Future[GoOnlineResponse]

  /**
   * sends command to the sequencer to go in Offline state if it is in Online state
   *
   * @return a [[esw.ocs.api.protocol.GoOfflineResponse]] as a Future value
   */
  def goOffline(): Future[GoOfflineResponse]

  /**
   * Discards all the pending steps of the sequence and call the abort handler of the sequencer's script
   *
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def abortSequence(): Future[OkOrUnhandledResponse]

  /**
   * Discards all the pending steps of the sequence and call the stop handler of the sequencer's script
   *
   * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as a Future value
   */
  def stop(): Future[OkOrUnhandledResponse]

  /**
   * Returns the current state of the sequencer (Idle, Loaded, Offline, Running, Processing)
   *
   * @return an [[esw.ocs.api.models.SequencerState]] as a Future value
   */
  def getSequencerState: Future[SequencerState]

  /**
   * Subscribes to the changes in state of sequencer which includes SequencerState (i.e Idle, Loaded, etc) and current StepList.
   *
   * @return a stream of current states with SequencerStateResponse as the materialized value which can be used to stop the subscription
   */
  def subscribeSequencerState(): Source[SequencerStateResponse, Subscription]

  /**
   * Sends command to the sequencer to call the diagnostic mode handler of the sequencer's script
   *
   * @param startTime - time at which the diagnostic mode will take effect
   * @param hint - String to support diagnostic data mode
   * @return a [[esw.ocs.api.protocol.DiagnosticModeResponse]] as a Future value
   */
  def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse]

  /**
   * Sends command to the sequencer to call the operations mode handler of the sequencer's script
   *
   * @return a [[esw.ocs.api.protocol.OperationsModeResponse]] as a Future value
   */
  def operationsMode(): Future[OperationsModeResponse]
}
