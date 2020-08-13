package esw.agent.service

import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.service.api.client.AgentServiceClientFactory
import esw.agent.service.api.models.{Killed, Spawned}
import esw.agent.service.app.AgentHttpWiring
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import esw.{BinaryFetcherUtil, GitUtil}

import scala.concurrent.duration.DurationInt

class AgentServiceIntegrationTest extends EswTestKit(AAS) {

  private val ocsAppVersion         = GitUtil.latestCommitSHA("esw")
  private val testCsChannel: String = BinaryFetcherUtil.eswChannel(ocsAppVersion)

  implicit val patience: PatienceConfig = PatienceConfig(1.minute)

  "AgentService" must {
    "start and shutdown sequence component on the given agent | ESW-361" in {

      //start agent service
      BinaryFetcherUtil.fetchBinaryFor(testCsChannel, Coursier.ocsApp(Some(ocsAppVersion)), Some(ocsAppVersion))
      val agentServiceWiring = new AgentHttpWiring(Some(4449))
      agentServiceWiring.httpService.startAndRegisterServer()

      //start agent
      val eswAgentPrefix = spawnAgent(AgentSettings(1.minute, testCsChannel), ESW)
      val seqCompName    = "ESW_1"
      val httpLocation   = resolveHTTPLocation(agentServiceWiring.prefix, ComponentType.Service)
      val seqCompPrefix  = Prefix(eswAgentPrefix.subsystem, seqCompName)

      val agentService = AgentServiceClientFactory(httpLocation, () => tokenWithEswUserRole())
      // spawn seq comp
      agentService.spawnSequenceComponent(eswAgentPrefix, seqCompName).futureValue shouldBe Spawned

      //verify component is started
      resolveSequenceComponent(seqCompPrefix)

      // stop spawned component
      agentService.stopComponent(eswAgentPrefix, ComponentId(seqCompPrefix, SequenceComponent)).futureValue shouldBe Killed

      val system = agentServiceWiring.actorSystem
      system.terminate()
      system.whenTerminated.futureValue
    }
  }

}
