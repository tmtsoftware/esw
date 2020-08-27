package esw.contract.data.agent

import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models.{KillResponse, SpawnResponse}
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{KillComponent, SpawnSequenceComponent, SpawnSequenceManager}

object AgentContract extends AgentServiceCodecs with AgentData {
  private val httpRequests = new RequestSet[AgentServiceRequest] {
    requestType(spawnSequenceComponent)
    requestType(killComponent)
    requestType(spawnSequenceManager)
  }

  private val models = ModelSet.models(
    ModelType[SpawnResponse](spawned, failed),
    ModelType[KillResponse](killed, failed)
  )

  private val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[SpawnSequenceComponent], name[SpawnResponse]),
    Endpoint(name[SpawnSequenceManager], name[SpawnResponse]),
    Endpoint(name[KillComponent], name[KillResponse])
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("agent-service/README.md"))

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract.empty,
    models = models,
    readme = readme
  )
}
