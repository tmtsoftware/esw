package esw.sm.app

import java.nio.file.Paths

import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Subsystem.{AOESW, ESW, IRIS, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.app.SequencerApp
import esw.ocs.testkit.EswTestKit
import esw.sm.api.SequenceManagerApi
import esw.sm.api.models.{CleanupResponse, ConfigureResponse}

class SequenceManagerIntegrationTest extends EswTestKit {

  override def afterEach(): Unit = locationService.unregisterAll()

  "configure and cleanup for provided observation mode | ESW-162, ESW-166" in {
    // Setup Sequence components for ESW, IRIS, AOESW
    TestSetup.setupSeqComponent(ESW, IRIS, AOESW)

    val obsMode: String = "IRIS_Cal"
    val configFilePath  = Paths.get(ClassLoader.getSystemResource("sequence_manager.conf").toURI)
    val wiring          = new SequenceManagerWiring(configFilePath)

    val sequenceManager: SequenceManagerApi = wiring.start

    // ************ Configure for observing mode ************************
    val configureResponse = sequenceManager.configure(obsMode).futureValue

    // assert for Successful Configuration
    configureResponse shouldBe a[ConfigureResponse.Success]

    // ESW-162 (verify all appropriate Sequencers are started based on observing mode)
    resolveSequencerLocation(Prefix(ESW, obsMode))
    resolveSequencerLocation(Prefix(IRIS, obsMode))
    resolveSequencerLocation(Prefix(AOESW, obsMode))

    // assert that sequence components are busy
    TestSetup.assertSeqCompAvailability(isSeqCompAvailable = false, ESW, IRIS, AOESW)

    // ESW-162 verify configure response returns master sequencer http location
    val masterSequencerLocation = resolveHTTPLocation(Prefix(ESW, obsMode), Sequencer)
    configureResponse shouldBe ConfigureResponse.Success(masterSequencerLocation)

    // *************** Cleanup for observing mode ********************
    val cleanupResponse = sequenceManager.cleanup(obsMode).futureValue

    // assert for Successful Cleanup
    cleanupResponse shouldBe CleanupResponse.Success

    // ESW-166 verify all sequencers are stopped for the observing mode and seq comps are available
    TestSetup.assertSeqCompAvailability(isSeqCompAvailable = true, ESW, IRIS, AOESW)
  }

  "configure should return error in case of conflicting resource | ESW-169" in {
    // Setup Sequence components for ESW, IRIS, AOESW and TCS
    TestSetup.setupSeqComponent(ESW, IRIS, AOESW, TCS)

    val configFilePath = Paths.get(ClassLoader.getSystemResource("sequence_manager.conf").toURI)
    val wiring         = new SequenceManagerWiring(configFilePath)

    // Start Sequence Manager
    val sequenceManager: SequenceManagerApi = wiring.start

    // Configure for "IRIS_Cal" observing mode should be successful as the resources are available
    sequenceManager.configure("IRIS_Cal").futureValue shouldBe a[ConfigureResponse.Success]

    // Configure for "IRIS_Darknight" observing mode should return error because resource IRIS and NFIRAOS are busy
    sequenceManager.configure("IRIS_Darknight").futureValue shouldBe ConfigureResponse.ConflictingResourcesWithRunningObsMode
  }

  object TestSetup {
    val seqCompName = "primary"

    def setupSeqComponent(subsystems: Subsystem*): Unit = {
      // Setup Sequence components for subsystems
      subsystems.foreach(subsystem => {
        SequencerApp.main(Array("seqcomp", "-s", subsystem.name, "-n", seqCompName))
      })
    }

    def assertSeqCompAvailability(isSeqCompAvailable: Boolean, subsystem: Subsystem*): Unit = {
      subsystem.foreach(subsystem => {
        val seqCompStatus =
          new SequenceComponentImpl(resolveSequenceComponentLocation(Prefix(subsystem, seqCompName))).status.futureValue
        if (isSeqCompAvailable) seqCompStatus.response shouldBe None // assert sequence components are available
        else seqCompStatus.response.isDefined shouldBe true          // assert sequence components are busy
      })
    }
  }
}
