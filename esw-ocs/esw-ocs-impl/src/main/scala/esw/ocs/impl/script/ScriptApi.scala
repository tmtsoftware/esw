package esw.ocs.impl.script

import org.apache.pekko.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime

import scala.concurrent.Future

/**
 * [Internal] This is the collection of APIs over sequencer-script.
 */
trait ScriptApi {

  /**
   * Executes the script's handler(handler with the same name as Command) with the command
   *
   * @param command - A sequence command to be used to call the handler
   * @return
   */
  def execute(command: SequenceCommand): Future[Unit]

  /**
   * Executes the script's onGoOnline handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeGoOnline(): Future[Done]

  /**
   * Executes the script's onGoOffline handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeGoOffline(): Future[Done]

  /**
   * Executes the script's onShutdown handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeShutdown(): Future[Done]

  /**
   * Executes the script's onAbortSequence handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeAbort(): Future[Done]

  /**
   * Executes the script's onNewSequence handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeNewSequenceHandler(): Future[Done]

  /**
   * Executes the script's onStop handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeStop(): Future[Done]

  /**
   * Executes the script's onDiagnosticMode handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done]

  /**
   * Executes the script's onOperationsMode handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeOperationsMode(): Future[Done]

  /**
   * Executes the script's onException handler
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def executeExceptionHandlers(ex: Throwable): Future[Done]

  /**
   * Runs the shutdown runnable(some extra tasks while unloading the script)
   *
   * @return a [[org.apache.pekko.Done]] as Future value
   */
  def shutdownScript(): Unit
}
