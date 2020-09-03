package esw.agent.akka

import java.nio.file.Paths

import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.Spawned
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS

import scala.concurrent.duration.DurationLong

class AgentIntegrationTest extends EswTestKit(AAS) with LocationServiceCodecs {

  private val agentPrefix: Prefix      = Prefix(ESW, "machine_A1")
  private var agentClient: AgentClient = _
  private val locationServiceUtil      = new LocationServiceUtil(locationService)

  LoggingSystemFactory.forTestingOnly()

  override def beforeAll(): Unit = {
    super.beforeAll()
    val channel: String = "file://" + getClass.getResource("/apps.json").getPath
    spawnAgent(AgentSettings(agentPrefix, 1.minute, channel))
    agentClient = AgentClient.make(agentPrefix, locationServiceUtil).rightValue
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  //ESW-325: spawns sequence component via agent using coursier with provided sha

  "Agent" must {

    "return Spawned on SpawnSequenceManager | ESW-180, ESW-366, ESW-367" in {
      val obsModeConfigPath = Paths.get(ClassLoader.getSystemResource("smObsModeConfig.conf").toURI)
      // spawn sequence manager
      val startKill = System.currentTimeMillis()
      agentClient.spawnSequenceManager(obsModeConfigPath, isConfigLocal = true, Some("1deb5acc58")).futureValue should ===(
        Spawned
      )
      println(s"*****************Spawn Sequence Manager*************************${System.currentTimeMillis() - startKill}")

      // Verify registration in location service
      val seqManagerConnection   = AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service))
      val location: AkkaLocation = locationService.resolve(seqManagerConnection, 5.seconds).futureValue.value

      // ESW-366 verify agent prefix and pid metadata is present in Sequence component akka location
      location.metadata.getAgentPrefix.get should ===(agentPrefix)
//      location.metadata.value.contains("PID") shouldBe true

      agentClient.killComponent(location).futureValue
    }
  }

  override def afterAll(): Unit = {
    locationService.unregisterAll()
    super.afterAll()
  }
}
