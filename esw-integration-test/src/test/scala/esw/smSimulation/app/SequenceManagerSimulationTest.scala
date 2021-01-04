package esw.smSimulation.app

import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer, Service}
import csw.location.api.models.Metadata
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{AOESW, ESW, IRIS, TCS}
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.protocol.{ConfigureResponse, ProvisionResponse, ShutdownSequenceComponentResponse, ShutdownSequencersResponse}

class SequenceManagerSimulationTest extends EswTestKit {

  "Sequence Manager in simulation mode" must {

    val prefix: Prefix = Prefix(ESW, "sequence_manager")

    "should shutdown all running seq comps and start new as given in provision config | ESW-174 " in {
      locationService.unregisterAll().futureValue

      val eswAgentPrefix  = Prefix(ESW, "machine1")
      val irisAgentPrefix = Prefix(IRIS, "machine1")
      val tcsAgentPrefix = Prefix(TCS, "machine1")

      // This step will call the SequenceManagerSimulation wiring which spawns a few agents and then
      // calls the start method of the SequenceManagerWiring
      val simulatedSequenceManager =
        SMSimulationTestSetup.startSequenceManagerSimulation(
          prefix,
          SMSimulationTestSetup.obsModeConfigPath,
          isConfigLocal = true,
          Some(eswAgentPrefix)
        )

      // spawning a sequence component before calling provision to check proper clean-up
      val eswRunningSeqComp = Prefix(ESW, "ESW_10")
      SMSimulationTestSetup.startSequenceComponents(eswRunningSeqComp)

      // Provision call
      val provisionConfig = ProvisionConfig(eswAgentPrefix -> 1, irisAgentPrefix -> 1, tcsAgentPrefix -> 1)
      simulatedSequenceManager.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      val eswNewSeqCompPrefix = Prefix(ESW, "ESW_1")
      val irisNewSeqComp      = Prefix(IRIS, "IRIS_1")
      val tcsNewSeqComp      = Prefix(TCS, "TCS_1")

      //verify seq comps are started as per the config
      val sequenceCompLocations = locationService.list(SequenceComponent).futureValue
      sequenceCompLocations.map(_.prefix) should not contain eswRunningSeqComp // ESW-358 verify the old seqComps are removed
      sequenceCompLocations.size shouldBe 2
      sequenceCompLocations.map(_.prefix) should contain allElementsOf List(eswNewSeqCompPrefix, irisNewSeqComp, tcsNewSeqComp)



      // Configure for observing mode
      val IRIS_CAL: ObsMode = ObsMode("IRIS_Cal")
      val eswIrisCalPrefix = Prefix(ESW, IRIS_CAL.name)
      val configureResponse = simulatedSequenceManager.configure(IRIS_CAL).futureValue

      // assert for Successful Configuration
      configureResponse shouldBe a[ConfigureResponse.Success]

      // verify configure response returns master sequencer ComponentId
      val masterSequencerLocation = resolveHTTPLocation(eswIrisCalPrefix, Sequencer)
      configureResponse should ===(ConfigureResponse.Success(masterSequencerLocation.connection.componentId))

      // configure another obs mode with conflicting resources should return error
      val IRIS_DARKNIGHT: ObsMode       = ObsMode("IRIS_Darknight")
      simulatedSequenceManager.configure(IRIS_DARKNIGHT).futureValue should ===(
        ConflictingResourcesWithRunningObsMode(Set(IRIS_CAL))
      )

      // Cleanup for observing mode
      val response = simulatedSequenceManager.shutdownObsModeSequencers(IRIS_CAL).futureValue

      // assert for Successful Cleanup
      response should ===(ShutdownSequencersResponse.Success)

      //clean up the provisioned sequence components
      simulatedSequenceManager.shutdownAllSequenceComponents().futureValue should ===(ShutdownSequenceComponentResponse.Success)
      SMSimulationTestSetup.cleanup()
    }

  }
}
