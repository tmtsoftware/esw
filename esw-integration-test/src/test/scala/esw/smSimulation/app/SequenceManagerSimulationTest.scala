package esw.smSimulation.app

import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ConfigureResponse.FailedToStartSequencers
import esw.sm.api.protocol.{ConfigureResponse, ProvisionResponse, ShutdownSequencersResponse}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

class SequenceManagerSimulationTest extends EswTestKit {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.minutes, 100.millis)

  "Sequence Manager in simulation mode" must {

    val prefix: Prefix = Prefix(ESW, "sequence_manager")

//    "should shutdown all running seq comps and start new as given in provision config | ESW-174 " in {
//      locationService.unregisterAll().futureValue
//
//      val eswAgentPrefix  = Prefix(ESW, "machine1")
//      val irisAgentPrefix = Prefix(IRIS, "machine1")
//      val tcsAgentPrefix  = Prefix(TCS, "machine1")
//
//      // This step will call the SequenceManagerSimulation wiring which spawns a few agents and then
//      // calls the start method of the SequenceManagerWiring
//      val simulatedSequenceManager =
//        SMSimulationTestSetup.startSequenceManagerSimulation(
//          prefix,
//          SMSimulationTestSetup.obsModeConfigPath,
//          isConfigLocal = true,
//          Some(eswAgentPrefix)
//        )
//
//      // spawning a sequence component before calling provision to check proper clean-up
//      val eswRunningSeqComp = Prefix(ESW, "ESW_10")
//      SMSimulationTestSetup.startSequenceComponents(eswRunningSeqComp)
//
//      // Provision call
//      val provisionConfig = ProvisionConfig(eswAgentPrefix -> 1, irisAgentPrefix -> 1, tcsAgentPrefix -> 1)
//      simulatedSequenceManager.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)
//
//      val eswNewSeqCompPrefix = Prefix(ESW, "ESW_1")
//      val irisNewSeqComp      = Prefix(IRIS, "IRIS_1")
//      val tcsNewSeqComp       = Prefix(TCS, "TCS_1")
//
//      //verify seq comps are started as per the config
//      val sequenceCompLocations = locationService.list(SequenceComponent).futureValue
//      sequenceCompLocations.map(_.prefix) should not contain eswRunningSeqComp // ESW-358 verify the old seqComps are removed
//      sequenceCompLocations.size shouldBe 3
//      sequenceCompLocations.map(_.prefix) should contain allElementsOf List(eswNewSeqCompPrefix, irisNewSeqComp, tcsNewSeqComp)
//
//      //clean up the provisioned sequence components
//      simulatedSequenceManager.shutdownAllSequenceComponents().futureValue should ===(ShutdownSequenceComponentResponse.Success)
//      SMSimulationTestSetup.cleanup()
//    }

    "configure with provision " in {

      locationService.unregisterAll().futureValue

      val eswAgentPrefix  = Prefix(ESW, "machine1")
      val irisAgentPrefix = Prefix(IRIS, "machine1")
      val tcsAgentPrefix  = Prefix(TCS, "machine1")

      val eswSeqCompPrefix  = Prefix(ESW, "ESW_1")
      val irisSeqCompPrefix = Prefix(IRIS, "IRIS_1")
      val tcsSeqCompPrefix  = Prefix(TCS, "TCS_1")

      // ESW-171, ESW-332: Starts SM and returns SM Http client which had ESW-user role.
      val simulatedSMClient = SMSimulationTestSetup.startSequenceManagerSimulation(
        prefix,
        SMSimulationTestSetup.obsModeConfigPath,
        isConfigLocal = true,
        Some(eswAgentPrefix)
      )

      val provisionConfig = ProvisionConfig(eswAgentPrefix -> 1, irisAgentPrefix -> 1, tcsAgentPrefix -> 1)
      //simulatedSMClient.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)
      println("Provision response ---------------------")
      simulatedSMClient.provision(provisionConfig).futureValue match {
        case ProvisionResponse.Success          => println("Success")
        case failure: ProvisionResponse.Failure => println(failure.getLocalizedMessage)
      }

      //  SMSimulationTestSetup.startSequenceComponents(eswSeqCompPrefix, irisSeqCompPrefix, tcsSeqCompPrefix)

      // ESW-366 verify SM Location metadata contains pid
      val smAkkaLocation = resolveAkkaLocation(prefix, Service)
      smAkkaLocation.metadata shouldBe Metadata().withPid(ProcessHandle.current().pid()).withAgentPrefix(eswAgentPrefix)

      val IRIS_Darknight: ObsMode = ObsMode("IRIS_darknight")
      val eswIrisCalPrefix        = Prefix(ESW, IRIS_Darknight.name)
      val irisCalPrefix           = Prefix(IRIS, IRIS_Darknight.name)
      val tcsIrisCalPrefix        = Prefix(TCS, IRIS_Darknight.name)

      // Configure for observing mode
      val configureResponse = simulatedSMClient.configure(IRIS_Darknight).futureValue

      // assert for Successful Configuration
      //configureResponse shouldBe a[ConfigureResponse.Success]
      (configureResponse.asInstanceOf[FailedToStartSequencers]).reasons.foreach(println)

      // verify configure response returns master sequencer ComponentId
      val masterSequencerLocation = resolveHTTPLocation(eswIrisCalPrefix, Sequencer)
      configureResponse should ===(ConfigureResponse.Success(masterSequencerLocation.connection.componentId))

      // verify all appropriate Sequencers are started based on observing mode
      resolveSequencerLocation(eswIrisCalPrefix).connection should ===(AkkaConnection(ComponentId(eswIrisCalPrefix, Sequencer)))
      resolveSequencerLocation(irisCalPrefix).connection should ===(AkkaConnection(ComponentId(irisCalPrefix, Sequencer)))
      resolveSequencerLocation(tcsIrisCalPrefix).connection should ===(AkkaConnection(ComponentId(tcsIrisCalPrefix, Sequencer)))

      // Cleanup for observing mode
      val response = simulatedSMClient.shutdownObsModeSequencers(IRIS_Darknight).futureValue

      // assert for Successful Cleanup
      response should ===(ShutdownSequencersResponse.Success)

      // verify all sequencers are stopped for the observing mode and seq comps are available
      val sequenceCompLocations = locationService.list(SequenceComponent).futureValue
      sequenceCompLocations.size shouldBe 3
      sequenceCompLocations.map(_.prefix) should contain allElementsOf List(eswSeqCompPrefix, irisSeqCompPrefix, tcsSeqCompPrefix)

      SMSimulationTestSetup.cleanup()
    }

  }
}
