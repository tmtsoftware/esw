package esw.contract.data.agentservice

import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers.*
import csw.contract.generator.*
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models.{AgentStatusResponse, KillResponse, SpawnResponse}
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{
  GetAgentStatus,
  KillComponent,
  SpawnSequenceComponent,
  SpawnSequenceManager
}

// ESW-376 Contract samples for agent service. These samples are also used in `RoundTripTest`
object AgentServiceContract extends AgentServiceCodecs with AgentContractData {
  private val httpRequests = new RequestSet[AgentServiceRequest] {
    requestType(spawnSequenceComponent)
    requestType(killComponent)
    requestType(spawnSequenceManager)
    requestType(getAgentStatus)
  }

  private val models = ModelSet.models(
    ModelType[SpawnResponse](spawned, failed),
    ModelType[KillResponse](killed, failed),
    ModelType[AgentStatusResponse](agentStatusSuccess, agentStatusFailure)
  )

  private val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[SpawnSequenceComponent], name[SpawnResponse]),
    Endpoint(name[SpawnSequenceManager], name[SpawnResponse]),
    Endpoint(name[KillComponent], name[KillResponse]),
    Endpoint(objectName(GetAgentStatus), name[AgentStatusResponse])
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("agent-service/README.md"))

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract.empty,
    models = models,
    readme = readme
  )
}
