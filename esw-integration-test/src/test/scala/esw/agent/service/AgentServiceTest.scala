package esw.agent.service

import java.nio.file.Path

import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.HttpConnection
import csw.testkit.ConfigTestKit
import esw.agent.akka.AgentSetup
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.client.AgentServiceClientFactory
import esw.agent.service.api.models.Spawned
import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS

import scala.concurrent.duration.DurationInt

class AgentServiceTest extends EswTestKit(AAS) with AgentSetup {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)
  private var agentClient: AgentClient                 = _
  private var agentService: AgentServiceApi            = _
  private var agentServiceWiring: AgentServiceWiring   = _
  private val locationServiceUtil                      = new LocationServiceUtil(locationService)

  override def beforeAll(): Unit = {
    // gateway setup
    super.beforeAll()
    // agent app setup
    spawnAgent(AgentSettings(agentPrefix, 1.minute, channel))
    agentClient = AgentClient.make(agentPrefix, locationServiceUtil).rightValue
    // agent service setup
    agentServiceWiring = AgentServiceApp.start(startLogging = false)
    val httpLocation = resolveHTTPLocation(agentServiceWiring.prefix, ComponentType.Service)
    agentService = AgentServiceClientFactory(httpLocation, () => tokenWithEswUserRole())
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    agentServiceWiring.stop().futureValue
    super.afterAll()
  }

  "AgentService" must {
//    "be able to spawn AAS" in {
//      val migrationFilePath = getClass.getResource("/realm-export.json").getPath
//      val keycloakDir = System.getProperty("user.home") + "/keycloak-11.0.3"
//
//      val spawnRes = agentService.spawnAAS(
//        agentPrefix,
//        Path.of(keycloakDir),
//        Path.of(migrationFilePath),
//        Some(8081),
//        Some("0.1.0-SNAPSHOT")
//      ).futureValue
//
//      spawnRes should ===(Spawned)
//
//      locationService.resolve(HttpConnection(ComponentId(Aget)))
//    }

    "be able to spawn EventServer" in {



      val migrationFilePath = getClass.getResource("/realm-export.json").getPath
      val keycloakDir = System.getProperty("user.home") + "/keycloak-11.0.3"

      val spawnRes = agentService.spawnAAS(
        agentPrefix,
        Path.of(keycloakDir),
        Path.of(migrationFilePath),
        Some(8081),
        Some("0.1.0-SNAPSHOT")
      ).futureValue

      spawnRes should ===(Spawned)

      locationService.resolve(HttpConnection(ComponentId(Aget)))
    }
  }

}
