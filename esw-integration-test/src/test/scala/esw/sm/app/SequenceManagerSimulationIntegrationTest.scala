package esw.sm.app

import java.nio.file.{Path, Paths}

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{AOESW, ESW, IRIS}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.akka.AgentSetup
import esw.agent.akka.app.AgentSettings
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ConfigureResponse.Success
import esw.sm.api.protocol.{ConfigureResponse, ShutdownSequencersResponse}

import scala.concurrent.duration.DurationInt

class SequenceManagerSimulationIntegrationTest extends EswTestKit(EventServer) with AgentSetup {

  private val sequenceManagerPrefix   = Prefix(ESW, "sequence_manager")
  private val obsModeConfigPath: Path = Paths.get(ClassLoader.getSystemResource("smObsModeConfigSimulation.conf").toURI)

  private val eswAgentPrefix   = getRandomAgentPrefix(ESW)
  private val aoeswAgentPrefix = getRandomAgentPrefix(AOESW)
  private val irisAgentPrefix  = getRandomAgentPrefix(IRIS)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(35.seconds, 50.millis)

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnAgent(AgentSettings(eswAgentPrefix, 1.minute, channel))
    spawnAgent(AgentSettings(aoeswAgentPrefix, 1.minute, channel))
    spawnAgent(AgentSettings(irisAgentPrefix, 1.minute, channel))
  }

  override def afterEach(): Unit = {
    super.afterEach()
    TestSetup.cleanup()
  }

  "Sequence Manager Simulation" must {
    "have ability be able to spawn sequencer hierarchy and send sequence to top level sequencer | ESW-146" in {
      val obsMode  = ObsMode("IRIS_Calib")
      val sequence = Sequence(Setup(sequenceManagerPrefix, CommandName("command-1"), None))

      val sequenceManager = TestSetup.startSequenceManager(sequenceManagerPrefix, obsModeConfigPath, simulation = true)

      val provisionConfig   = ProvisionConfig(eswAgentPrefix -> 1, aoeswAgentPrefix -> 1, irisAgentPrefix -> 1)
      val provisionResponse = sequenceManager.provision(provisionConfig).futureValue
      println(provisionResponse)
      // verify ESW sequencer is considered as top level sequencer
      val configureResponse = sequenceManager.configure(obsMode).futureValue
      configureResponse should ===(ConfigureResponse.Success(ComponentId(Prefix(ESW, obsMode.name), Sequencer)))

      val successResponse = configureResponse.asInstanceOf[Success]
      val id              = successResponse.masterSequencerComponentId

      val location = resolveHTTPLocation(id.prefix, id.componentType)

      // ESW-146 : Send Sequence to master sequencer. (TestScript5 is loaded in master sequencer)
      SequencerApiFactory.make(location).submitAndWait(sequence).futureValue shouldBe a[Completed]

      sequenceManager.shutdownObsModeSequencers(obsMode).futureValue shouldBe a[ShutdownSequencersResponse.Success.type]
    }
  }
}
