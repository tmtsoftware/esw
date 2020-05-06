package esw.sm.app

import java.nio.file.Paths

import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{AOESW, ESW, IRIS}
import esw.ocs.app.SequencerApp
import esw.ocs.testkit.EswTestKit
import esw.sm.api.actor.client.SequenceManagerImpl
import esw.sm.api.models.ConfigureResponse

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SequenceManagerIntegrationTest extends EswTestKit {

  LoggingSystemFactory.forTestingOnly()

  "configure for provided observation mode | ESW-178" in {
    SequencerApp.main(Array("seqcomp", "-s", "esw", "-n", "primary"))
    SequencerApp.main(Array("seqcomp", "-s", "iris", "-n", "primary"))
    SequencerApp.main(Array("seqcomp", "-s", "aoesw", "-n", "primary"))

    val obsMode: String = "IRIS_cal"
    val configFilePath  = Paths.get(ClassLoader.getSystemResource("sequence_manager.conf").toURI)

    val wiring                               = new SequenceManagerWiring(configFilePath)
    val sequenceManager: SequenceManagerImpl = wiring.start

    val configureResponse = Await.result(sequenceManager.configure(obsMode), 10.seconds)

    configureResponse shouldBe a[ConfigureResponse.Success]

    // verify ESW_IRIS_cal Sequencer is started
    resolveSequencerLocation(Prefix(ESW, obsMode))
    resolveSequencerLocation(Prefix(IRIS, obsMode))
    resolveSequencerLocation(Prefix(AOESW, obsMode))
  }
}
