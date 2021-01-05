package esw.smSimulation.app

import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Metadata
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ConfigureResponse.FailedToStartSequencers
import esw.sm.api.protocol.{ConfigureResponse, ProvisionResponse}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

class SequenceManagerSimulationTest extends EswTestKit {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.minutes, 100.millis)

  "Sequence Manager in simulation mode" must {

    val prefix: Prefix = Prefix(ESW, "sequence_manager")

    "configure with provision " in {
      locationService.unregisterAll().futureValue

      val eswAgentPrefix  = Prefix(ESW, "machine1")
      val irisAgentPrefix = Prefix(IRIS, "machine1")
      val tcsAgentPrefix  = Prefix(TCS, "machine1")

      val simulatedSMClient = SMSimulationTestSetup.startSequenceManagerSimulation(
        prefix,
        SMSimulationTestSetup.obsModeConfigPath,
        isConfigLocal = true,
        Some(eswAgentPrefix)
      )

      val provisionConfig   = ProvisionConfig(eswAgentPrefix -> 1, irisAgentPrefix -> 1, tcsAgentPrefix -> 1)
      val provisionResponse = simulatedSMClient.provision(provisionConfig).futureValue
      provisionResponse should ===(ProvisionResponse.Success)

      val smAkkaLocation = resolveAkkaLocation(prefix, Service)
      smAkkaLocation.metadata shouldBe Metadata().withPid(ProcessHandle.current().pid()).withAgentPrefix(eswAgentPrefix)

      val IRIS_Darknight: ObsMode = ObsMode("IRIS_Darknight")

      val configureResponse = simulatedSMClient.configure(IRIS_Darknight).futureValue

      configureResponse match {
        case ConfigureResponse.Success(_)       => println(">>>>>>>>>>> ConfigureResponse Success >>>>>>>>>>>>>>>>>>>>>>")
        case failure: FailedToStartSequencers   => println(s">>>>>>>>>>>>> ConfigureResponse.Failure ${failure.reasons}")
        case failure: ConfigureResponse.Failure => println(s">>>>>>>>>>>>> ConfigureResponse.Failure ${failure}")
      }

      SMSimulationTestSetup.cleanup()
    }

  }
}
