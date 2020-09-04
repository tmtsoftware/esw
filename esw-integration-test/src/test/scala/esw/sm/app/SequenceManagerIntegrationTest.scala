package esw.sm.app

import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import esw.agent.akka.app.AgentSettings
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.client.AgentServiceClientFactory
import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import esw.sm.api.models.{AgentStatus, ProvisionConfig, SequenceComponentStatus}
import esw.sm.api.protocol._

import scala.concurrent.duration.DurationInt

class SequenceManagerIntegrationTest extends EswTestKit(AAS) {
  private val IRIS_DARKNIGHT        = ObsMode("IRIS_Darknight")
  private val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
  private val ocsAppVersion         = "0.1.0-SNAPSHOT"
  private val testCsChannel: String = "file://" + getClass.getResource("/apps.json").getPath

  private var agentService: AgentServiceApi          = _
  private var agentServiceWiring: AgentServiceWiring = _
  private val ocsVersionOpt                          = Some(ocsAppVersion)

  override def beforeAll(): Unit = {
    super.beforeAll()
    agentServiceWiring = AgentServiceApp.start()
    val httpLocation = resolveHTTPLocation(agentServiceWiring.prefix, ComponentType.Service)
    agentService = AgentServiceClientFactory(httpLocation, () => tokenWithEswUserRole())
  }

  override protected def beforeEach(): Unit = {
    locationService.unregisterAll().futureValue
    registerKeycloak()
  }

  override protected def afterEach(): Unit = {
    TestSetup.cleanup()
    super.afterEach()
  }

  override def afterAll(): Unit = {
    agentServiceWiring.stop().futureValue
    super.afterAll()
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  "provision should shutdown all running seq comps and start new as given in provision config | ESW-347, ESW-358, ESW-332" in {
    val eswAgentPrefix  = getRandomAgentPrefix(ESW)
    val irisAgentPrefix = getRandomAgentPrefix(IRIS)
    // start required agents to provision and verify they are running
    spawnAgent(AgentSettings(eswAgentPrefix, 1.minute, testCsChannel))
    spawnAgent(AgentSettings(irisAgentPrefix, 1.minute, testCsChannel))

    val eswRunningSeqComp = Prefix(ESW, "ESW_10")
    TestSetup.startSequenceComponents(eswRunningSeqComp)

    val provisionConfig = ProvisionConfig(eswAgentPrefix -> 1, irisAgentPrefix -> 1)
    val sequenceManager = TestSetup.startSequenceManagerAuthEnabled(sequenceManagerPrefix, tokenWithEswUserRole)
    val startProvision  = System.currentTimeMillis()
    sequenceManager.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)
    println("*************Provision****************" + (System.currentTimeMillis() - startProvision))

    val eswNewSeqCompPrefix = Prefix(ESW, "ESW_1")
    val irisNewSeqComp      = Prefix(IRIS, "IRIS_1")
    //verify seq comps are started as per the config
    val sequenceCompLocations = locationService.list(SequenceComponent).futureValue
    sequenceCompLocations.map(_.prefix) should not contain eswRunningSeqComp // ESW-358 verify the old seqComps are removed
    sequenceCompLocations.size shouldBe 2
    sequenceCompLocations.map(_.prefix) should contain allElementsOf List(eswNewSeqCompPrefix, irisNewSeqComp)

    //clean up the provisioned sequence components
    sequenceManager.shutdownAllSequenceComponents().futureValue should ===(ShutdownSequenceComponentResponse.Success)
  }

  "getAgentStatus should return status for running sequence components and loaded scripts | ESW-349, ESW-332, ESW-367" in {

    val eswAgentPrefix  = getRandomAgentPrefix(ESW)
    val irisAgentPrefix = getRandomAgentPrefix(IRIS)
    // start required agents
    spawnAgent(AgentSettings(eswAgentPrefix, 1.minute, testCsChannel))
    spawnAgent(AgentSettings(irisAgentPrefix, 1.minute, testCsChannel))

    val sequenceManager = TestSetup.startSequenceManagerAuthEnabled(sequenceManagerPrefix, tokenWithEswUserRole)

    agentService.spawnSequenceComponent(eswAgentPrefix, "primary", ocsVersionOpt).futureValue
    agentService.spawnSequenceComponent(irisAgentPrefix, "primary", ocsVersionOpt).futureValue

    sequenceManager.startSequencer(IRIS, IRIS_DARKNIGHT).futureValue

    val sequencerLocation = resolveSequencerLocation(IRIS, IRIS_DARKNIGHT)

    val expectedStatus = Set(
      AgentStatus(
        ComponentId(irisAgentPrefix, Machine),
        List(
          SequenceComponentStatus(ComponentId(Prefix(IRIS, "primary"), SequenceComponent), Some(sequencerLocation))
        )
      ),
      AgentStatus(
        ComponentId(eswAgentPrefix, Machine),
        List(
          SequenceComponentStatus(ComponentId(Prefix(ESW, "primary"), SequenceComponent), None)
        )
      )
    )
    val startGetAgentStatus = System.currentTimeMillis()
    val actualResponse      = sequenceManager.getAgentStatus.futureValue.asInstanceOf[AgentStatusResponse.Success]
    actualResponse.agentStatus.toSet should ===(expectedStatus)
    println("*************Get Agent Status****************" + (System.currentTimeMillis() - startGetAgentStatus))
    actualResponse.seqCompsWithoutAgent should ===(List.empty)

    sequenceManager.shutdownAllSequenceComponents().futureValue
  }

}
