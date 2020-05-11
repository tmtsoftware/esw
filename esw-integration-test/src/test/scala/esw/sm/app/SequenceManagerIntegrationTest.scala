package esw.sm.app

import java.nio.file.Paths

import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.app.SequencerApp
import esw.ocs.testkit.EswTestKit
import esw.sm.api.SequenceManagerApi
import esw.sm.api.models.{CleanupResponse, ConfigureResponse}

class SequenceManagerIntegrationTest extends EswTestKit {

  override def afterEach(): Unit = locationService.unregisterAll()

  "configure and cleanup for provided observation mode | ESW-162, ESW-166, ESW-164" in {
    TestSetup.setupSeqComponent(Prefix(ESW, "primary"), Prefix(IRIS, "primary"), Prefix(AOESW, "primary"))
    val sequenceManager = TestSetup.startSequenceManager()
    val obsMode: String = "IRIS_Cal"

    // ************ Configure for observing mode ************************
    val configureResponse = sequenceManager.configure(obsMode).futureValue

    // assert for Successful Configuration
    configureResponse shouldBe a[ConfigureResponse.Success]

    // ESW-162 (verify all appropriate Sequencers are started based on observing mode)
    resolveSequencerLocation(Prefix(ESW, obsMode))
    resolveSequencerLocation(Prefix(IRIS, obsMode))
    resolveSequencerLocation(Prefix(AOESW, obsMode))

    // ESW-164 assert that sequence components have loaded sequencer scripts
    TestSetup.assertSeqCompAvailability(
      isSeqCompAvailable = false,
      Prefix(ESW, "primary"),
      Prefix(IRIS, "primary"),
      Prefix(AOESW, "primary")
    )

    // ESW-162 verify configure response returns master sequencer http location
    val masterSequencerLocation =
      resolveHTTPLocation(Prefix(ESW, obsMode), Sequencer)
    configureResponse shouldBe ConfigureResponse.Success(masterSequencerLocation)

    // *************** Cleanup for observing mode ********************
    val cleanupResponse = sequenceManager.cleanup(obsMode).futureValue

    // assert for Successful Cleanup
    cleanupResponse shouldBe CleanupResponse.Success

    // ESW-166 verify all sequencers are stopped for the observing mode and seq comps are available
    TestSetup.assertSeqCompAvailability(
      isSeqCompAvailable = true,
      Prefix(ESW, "primary"),
      Prefix(IRIS, "primary"),
      Prefix(AOESW, "primary")
    )
  }

  "configure should run multiple sequencers efficiently | ESW-168, ESW-169" in {
    TestSetup.setupSeqComponent(
      Prefix(ESW, "primary"),
      Prefix(ESW, "secondary"),
      Prefix(IRIS, "primary"),
      Prefix(AOESW, "primary"),
      Prefix(WFOS, "primary"),
      Prefix(TCS, "primary")
    )
    val sequenceManager = TestSetup.startSequenceManager()

    // Configure for "IRIS_Cal" observing mode should be successful as the resources are available
    sequenceManager
      .configure("IRIS_Cal")
      .futureValue shouldBe a[ConfigureResponse.Success]

    // *************** Avoid conflicting sequence execution | ESW-169 ********************
    // Configure for "IRIS_Darknight" observing mode should return error because resource IRIS and NFIRAOS are busy
    sequenceManager
      .configure("IRIS_Darknight")
      .futureValue shouldBe ConfigureResponse.ConflictingResourcesWithRunningObsMode

    // *************** Should run observation concurrently if no conflict in resources | ESW-168 ********************
    // Configure for "WFOS_Cal" observing mode should be successful as the resources are available
    sequenceManager
      .configure("WFOS_Cal")
      .futureValue shouldBe a[ConfigureResponse.Success]

    // Test cleanup
    sequenceManager.cleanup("IRIS_Cal").futureValue
    sequenceManager.cleanup("WFOS_Cal").futureValue
  }

  object TestSetup {
    def setupSeqComponent(prefixes: Prefix*): Unit = {
      // Setup Sequence components for subsystems
      prefixes.foreach(prefix => {
        SequencerApp.main(Array("seqcomp", "-s", prefix.subsystem.name, "-n", prefix.componentName))
      })
    }

    def assertSeqCompAvailability(isSeqCompAvailable: Boolean, prefixes: Prefix*): Unit = {
      prefixes.foreach(prefix => {
        val seqCompStatus =
          new SequenceComponentImpl(resolveSequenceComponentLocation(prefix)).status.futureValue
        if (isSeqCompAvailable)
          seqCompStatus.response shouldBe None // assert sequence components are available
        else
          seqCompStatus.response.isDefined shouldBe true // assert sequence components are busy
      })
    }

    def startSequenceManager(): SequenceManagerApi = {
      val configFilePath =
        Paths.get(ClassLoader.getSystemResource("sequence_manager.conf").toURI)
      val wiring          = new SequenceManagerWiring(configFilePath)
      val sequenceManager = wiring.start
      sequenceManager
    }
  }
}
