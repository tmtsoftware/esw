package esw.contract.data

import csw.contract.generator.Services
import esw.contract.data.agentservice.AgentServiceContract
import esw.contract.data.gateway.GatewayContract
import esw.contract.data.sequencemanager.SequenceManagerContract
import esw.contract.data.sequencer.SequencerContract

/**
 * Model representing contract Data for all public ESW services
 */
object EswData {
  val services: Services = Services(
    Map(
      "sequencer-service"        -> SequencerContract.service,
      "gateway-service"          -> GatewayContract.service,
      "sequence-manager-service" -> SequenceManagerContract.service,
      "agent-service"            -> AgentServiceContract.service
    )
  )
}
