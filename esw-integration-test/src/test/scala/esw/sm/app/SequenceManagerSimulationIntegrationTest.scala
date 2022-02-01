package esw.sm.app

import csw.config.api.scaladsl.ConfigService
import csw.config.api.{ConfigData, TokenFactory}
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import csw.testkit.scaladsl.CSWService.{ConfigServer, EventServer}
import esw.agent.akka.AgentSetup
import esw.agent.akka.app.AgentSettings
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ConfigureResponse.Success
import esw.sm.api.protocol.{ConfigureResponse, ProvisionResponse, ShutdownSequenceComponentResponse, ShutdownSequencersResponse}
import org.mockito.Mockito.when

import java.nio.file.{Path, Paths}
import scala.concurrent.duration.DurationInt

class SequenceManagerSimulationIntegrationTest extends EswTestKit(EventServer, ConfigServer) with AgentSetup {

  private val sequenceManagerPrefix   = Prefix(ESW, "sequence_manager")
  private val obsModeConfigPath: Path = Paths.get(ClassLoader.getSystemResource("smObsModeConfigSimulation.conf").toURI)

  private val eswAgentPrefix  = getRandomAgentPrefix(ESW)
  private val tcsAgentPrefix  = getRandomAgentPrefix(TCS)
  private val irisAgentPrefix = getRandomAgentPrefix(IRIS)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(35.seconds, 50.millis)

  override def beforeAll(): Unit = {
    super.beforeAll()

    val factory = mock[TokenFactory]
    when(factory.getToken).thenReturn("validToken")
    val adminApi: ConfigService = ConfigClientFactory.adminApi(actorSystem, locationService, factory)

    val configFilePath = Path.of("/tmt/osw/version.conf")
    val scriptVersionConf: String =
      """
        | scripts = 0.1.0-SNAPSHOT
        |""".stripMargin
    // create obsMode config file on config server
    adminApi.create(configFilePath, ConfigData.fromString(scriptVersionConf), annex = false, "First commit").futureValue

    spawnAgent(AgentSettings(eswAgentPrefix, channel, configFilePath))
    spawnAgent(AgentSettings(tcsAgentPrefix, channel, configFilePath))
    spawnAgent(AgentSettings(irisAgentPrefix, channel, configFilePath))
  }

  override def afterEach(): Unit = {
    super.afterEach()
    TestSetup.cleanup()
  }

  "Sequence Manager Simulation" must {

    "start and register akka and http locations | ESW-174" in {

      // resolving sequence manager fails for Akka and Http
      intercept[Exception](resolveAkkaLocation(sequenceManagerPrefix, Service))
      intercept[Exception](resolveHTTPLocation(sequenceManagerPrefix, Service))

      TestSetup.startSequenceManager(
        sequenceManagerPrefix,
        obsModeConfigPath,
        isConfigLocal = true,
        Some(eswAgentPrefix),
        simulation = true
      )

      val smAkkaLocation = resolveAkkaLocation(sequenceManagerPrefix, Service)
      smAkkaLocation.prefix shouldBe sequenceManagerPrefix

      //  verify agent prefix and pid metadata is present in Sequence manager akka location
      smAkkaLocation.metadata.getAgentPrefix shouldBe Some(eswAgentPrefix)
      smAkkaLocation.metadata.getPid.get shouldNot equal(None)

      val smHttpLocation = resolveHTTPLocation(sequenceManagerPrefix, Service)
      smHttpLocation.prefix shouldBe sequenceManagerPrefix

      // verify agent prefix and pid metadata is present in Sequence manager http location
      smHttpLocation.metadata.getAgentPrefix shouldBe Some(eswAgentPrefix)
      smHttpLocation.metadata.getPid.get shouldNot equal(None)

    }

    "be able to add component's entries in the location service on receiving commands like provision, configure, etc| ESW-174" in {

      val eswRunningSeqComp = Prefix(ESW, "ESW_10")
      TestSetup.startSequenceComponents(eswRunningSeqComp)

      val provisionConfig = ProvisionConfig(eswAgentPrefix -> 1, irisAgentPrefix -> 1, tcsAgentPrefix -> 1)
      val sequenceManager = TestSetup.startSequenceManager(sequenceManagerPrefix, obsModeConfigPath, simulation = true)
      sequenceManager.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      val eswNewSeqCompPrefix = Prefix(ESW, "ESW_1")
      val irisNewSeqComp      = Prefix(IRIS, "IRIS_1")
      val tcsNewSeqComp       = Prefix(TCS, "TCS_1")

      // verify seq comps are started as per the config
      val sequenceCompLocations = locationService.list(SequenceComponent).futureValue
      sequenceCompLocations.map(_.prefix) should not contain eswRunningSeqComp
      sequenceCompLocations.size shouldBe 3
      sequenceCompLocations.map(_.prefix) should contain allElementsOf List(eswNewSeqCompPrefix, irisNewSeqComp, tcsNewSeqComp)

      // verify the ability to spawn sequencer hierarchy for the provided obsmode with no provided script
      val obsMode  = ObsMode("IRIS_NOScript")
      val sequence = Sequence(Setup(sequenceManagerPrefix, CommandName("command-1"), None))

      val eswIrisCalPrefix = Prefix(ESW, obsMode.name)
      val irisCalPrefix    = Prefix(IRIS, obsMode.name)
      val tcsIrisCalPrefix = Prefix(TCS, obsMode.name)

      val configureResponse       = sequenceManager.configure(obsMode).futureValue
      val masterSequencerLocation = resolveHTTPLocation(eswIrisCalPrefix, Sequencer)
      configureResponse should ===(ConfigureResponse.Success(masterSequencerLocation.connection.componentId))

      val successResponse = configureResponse.asInstanceOf[Success]
      val id              = successResponse.masterSequencerComponentId
      val location        = resolveHTTPLocation(id.prefix, id.componentType)

      val sequencerLocations = locationService.list(Sequencer).futureValue
      sequencerLocations.size shouldBe 6

      resolveSequencerLocation(eswIrisCalPrefix).connection should ===(AkkaConnection(ComponentId(eswIrisCalPrefix, Sequencer)))
      resolveSequencerLocation(irisCalPrefix).connection should ===(AkkaConnection(ComponentId(irisCalPrefix, Sequencer)))
      resolveSequencerLocation(tcsIrisCalPrefix).connection should ===(AkkaConnection(ComponentId(tcsIrisCalPrefix, Sequencer)))

      // verify sending a sequence to master sequencer returns completed
      SequencerApiFactory.make(location).submitAndWait(sequence).futureValue shouldBe a[Completed]

      // shutting down obsmode sequencers must return success
      sequenceManager.shutdownObsModeSequencers(obsMode).futureValue should ===(ShutdownSequencersResponse.Success)

      // shutting down obs-mode sequencers should remove their entries from location service.
      intercept[Exception](resolveAkkaLocation(eswIrisCalPrefix, Sequencer))
      intercept[Exception](resolveAkkaLocation(irisCalPrefix, Sequencer))
      intercept[Exception](resolveAkkaLocation(tcsIrisCalPrefix, Sequencer))

      // shutdown provided sequence component
      sequenceManager.shutdownSequenceComponent(eswNewSeqCompPrefix).futureValue should ===(
        ShutdownSequenceComponentResponse.Success
      )

      // verify that the sequence component's location has been removed from the location service
      val sequenceCompLocationNew = locationService.list(SequenceComponent).futureValue
      sequenceCompLocationNew.map(_.prefix) should not contain eswNewSeqCompPrefix
      sequenceCompLocationNew.size shouldBe 2

      // clean up the provisioned sequence components
      sequenceManager.shutdownAllSequenceComponents().futureValue should ===(ShutdownSequenceComponentResponse.Success)
    }
  }
}
