package esw.ocs.impl.blockhound

import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

/**
 * An object holding the list of BlockHoundIntegration to be used in Sequencer to detect blocking calls from non-blocking threads
 */
object BlockHoundWiring {
  var integrations = List.empty[BlockHoundIntegration]

  def addIntegration(integration: BlockHoundIntegration): Unit = {
    integrations = integration +: integrations
  }

  def install(): Unit = {
    BlockHound.install(integrations*)
  }
}
