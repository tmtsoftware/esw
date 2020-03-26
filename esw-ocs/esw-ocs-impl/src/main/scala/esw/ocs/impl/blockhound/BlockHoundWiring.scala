package esw.ocs.impl.blockhound

import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

object BlockHoundWiring {
  var integrations = List.empty[BlockHoundIntegration]

  def addIntegration(integration: BlockHoundIntegration): Unit = {
    integrations = integration +: integrations
  }

  def install(): Unit = {
    BlockHound.install(integrations: _*)
  }
}
