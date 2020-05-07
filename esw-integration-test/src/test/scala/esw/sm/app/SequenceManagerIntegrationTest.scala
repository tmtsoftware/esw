package esw.sm.app

import java.nio.file.Paths

import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Subsystem.{AOESW, ESW, IRIS}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.app.SequencerApp
import esw.ocs.testkit.EswTestKit
import esw.sm.api.SequenceManagerApi
import esw.sm.api.models.ConfigureResponse

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SequenceManagerIntegrationTest extends EswTestKit {

  override def afterEach(): Unit = locationService.unregisterAll()

  "configure for provided observation mode | ESW-162" in {
    // Setup Sequence components for ESW, IRIS, AOESW
    TestSetup.setupSeqComponent(ESW, IRIS, AOESW)

    val obsMode: String = "IRIS_Cal"
    val configFilePath  = Paths.get(ClassLoader.getSystemResource("sequence_manager.conf").toURI)
    val wiring          = new SequenceManagerWiring(configFilePath)

    // Start Sequence Manager
    val sequenceManager: SequenceManagerApi = wiring.start

    // Configure for observing mode
    val configureResponse = Await.result(sequenceManager.configure(obsMode), 10.seconds)

    // assert for Successful Configuration
    configureResponse shouldBe a[ConfigureResponse.Success]

    // ESW-162 (verify all appropriate Sequencers are started based on observing mode)
    resolveSequencerLocation(Prefix(ESW, obsMode))
    resolveSequencerLocation(Prefix(IRIS, obsMode))
    resolveSequencerLocation(Prefix(AOESW, obsMode))

    // verify configure response returns master sequencer http location
    val masterSequencerLocation = resolveHTTPLocation(Prefix(ESW, obsMode), Sequencer)
    configureResponse shouldBe ConfigureResponse.Success(masterSequencerLocation)
  }

  object TestSetup {
    def setupSeqComponent(subsystems: Subsystem*): Unit = {
      // Setup Sequence components for subsystems
      subsystems.foreach(subsystem => SequencerApp.main(Array("seqcomp", "-s", subsystem.name, "-n", "primary")))
    }
  }
}
