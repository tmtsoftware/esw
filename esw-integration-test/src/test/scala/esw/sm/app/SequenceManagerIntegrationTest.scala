package esw.sm.app

import java.nio.file.Paths

import akka.actor.CoordinatedShutdown
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Sequencer, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerImpl}
import esw.ocs.app.SequencerApp
import esw.ocs.app.SequencerAppCommand.SequenceComponent
import esw.ocs.app.wiring.SequenceComponentWiring
import esw.ocs.testkit.EswTestKit
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl
import esw.sm.api.models.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.models.SequenceManagerError.LoadScriptError
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, ShutdownSequencerResponse, StartSequencerResponse}
import esw.sm.app.SequenceManagerAppCommand.StartCommand

import scala.collection.mutable.ArrayBuffer

class SequenceManagerIntegrationTest extends EswTestKit {
  private val WFOS_CAL              = "WFOS_Cal"
  private val IRIS_CAL              = "IRIS_Cal"
  private val IRIS_DARKNIGHT        = "IRIS_Darknight"
  private val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")

  override protected def beforeEach(): Unit = locationService.unregisterAll()
  override protected def afterEach(): Unit  = TestSetup.cleanup()

  "configure and cleanup for provided observation mode | ESW-162, ESW-166, ESW-164, ESW-172" in {
    val eswSeqCompPrefix   = Prefix(ESW, "primary")
    val irisSeqCompPrefix  = Prefix(IRIS, "primary")
    val aoeswSeqCompPrefix = Prefix(AOESW, "primary")

    TestSetup.startSequenceComponents(eswSeqCompPrefix, irisSeqCompPrefix, aoeswSeqCompPrefix)

    //ESW-172 resolving sequence manager fails
    intercept[Exception](resolveAkkaLocation(sequenceManagerPrefix, Service))

    val sequenceManager = TestSetup.startSequenceManager()

    //ESW-172 verify sequence manager is registered with location service
    resolveAkkaLocation(sequenceManagerPrefix, Service).prefix shouldBe sequenceManagerPrefix

    val eswIrisCalPrefix   = Prefix(ESW, IRIS_CAL)
    val irisCalPrefix      = Prefix(IRIS, IRIS_CAL)
    val aoeswIrisCalPrefix = Prefix(AOESW, IRIS_CAL)

    // ************ Configure for observing mode ************************
    val configureResponse = sequenceManager.configure(IRIS_CAL).futureValue

    // assert for Successful Configuration
    configureResponse shouldBe a[ConfigureResponse.Success]

    // ESW-162 verify configure response returns master sequencer ComponentId
    val masterSequencerLocation = resolveHTTPLocation(eswIrisCalPrefix, Sequencer)
    configureResponse should ===(ConfigureResponse.Success(masterSequencerLocation.connection.componentId))

    // ESW-162 (verify all appropriate Sequencers are started based on observing mode)
    resolveSequencerLocation(eswIrisCalPrefix).connection should ===(sequencerConnection(eswIrisCalPrefix))
    resolveSequencerLocation(irisCalPrefix).connection should ===(sequencerConnection(irisCalPrefix))
    resolveSequencerLocation(aoeswIrisCalPrefix).connection should ===(sequencerConnection(aoeswIrisCalPrefix))

    // ESW-164 assert that sequence components have loaded sequencer scripts
    assertThatSeqCompIsLoadedWithScript(eswSeqCompPrefix)
    assertThatSeqCompIsLoadedWithScript(irisSeqCompPrefix)
    assertThatSeqCompIsLoadedWithScript(aoeswSeqCompPrefix)

    // *************** Cleanup for observing mode ********************
    val cleanupResponse = sequenceManager.cleanup(IRIS_CAL).futureValue

    // assert for Successful Cleanup
    cleanupResponse should ===(CleanupResponse.Success)

    // ESW-166 verify all sequencers are stopped for the observing mode and seq comps are available
    assertThatSeqCompIsAvailable(eswSeqCompPrefix)
    assertThatSeqCompIsAvailable(irisSeqCompPrefix)
    assertThatSeqCompIsAvailable(aoeswSeqCompPrefix)
  }

  "configure should run multiple obs modes in parallel if resources are not conflicting | ESW-168, ESW-169" in {
    TestSetup.startSequenceComponents(
      Prefix(ESW, "primary"),
      Prefix(ESW, "secondary"),
      Prefix(IRIS, "primary"),
      Prefix(AOESW, "primary"),
      Prefix(WFOS, "primary"),
      Prefix(TCS, "primary")
    )
    val sequenceManager = TestSetup.startSequenceManager()

    // Configure for "IRIS_Cal" observing mode should be successful as the resources are available
    sequenceManager.configure(IRIS_CAL).futureValue shouldBe a[ConfigureResponse.Success]

    // *************** Avoid conflicting sequence execution | ESW-169 ********************
    // Configure for "IRIS_Darknight" observing mode should return error because resource IRIS and NFIRAOS are busy
    sequenceManager.configure(IRIS_DARKNIGHT).futureValue should ===(ConflictingResourcesWithRunningObsMode(Set(IRIS_CAL)))

    // *************** Should run observation concurrently if no conflict in resources | ESW-168 ********************
    // Configure for "WFOS_Cal" observing mode should be successful as the resources are available
    sequenceManager.configure(WFOS_CAL).futureValue shouldBe a[ConfigureResponse.Success]

    // Test cleanup
    sequenceManager.cleanup(IRIS_CAL).futureValue
    sequenceManager.cleanup(WFOS_CAL).futureValue
  }

  "start sequencer on esw sequence component as fallback if subsystem sequence component is not available | ESW-164" in {
    TestSetup.startSequenceComponents(Prefix(ESW, "primary"), Prefix(ESW, "secondary"), Prefix(IRIS, "primary"))
    val sequenceManager = TestSetup.startSequenceManager()

    // ************ Configure for observing mode: sequencers required: [IRIS, ESW, AOESW] ************************
    sequenceManager.configure(IRIS_CAL).futureValue shouldBe a[ConfigureResponse.Success]

    val aoeswSequencer          = resolveSequencer(AOESW, IRIS_CAL)
    val seqCompRunningSequencer = new SequencerImpl(aoeswSequencer).getSequenceComponent.futureValue

    // ESW-164 verify AOESW.IRIS_CAL sequencer is running on fallback ESW sequence component as AOESW sequence component
    // is not available
    seqCompRunningSequencer.prefix.subsystem shouldBe ESW

    //test cleanup
    sequenceManager.cleanup(IRIS_CAL)
  }

  "throw exception if config file is missing | ESW-162" in {
    val exception = intercept[RuntimeException](SequenceManagerApp.main(Array("start", "-p", "sm-config.conf")))
    exception.getMessage shouldBe "File does not exist on local disk at path sm-config.conf"
  }

  "start and shut down sequencer for given subsystem and observation mode | ESW-176, ESW-326" in {
    TestSetup.startSequenceComponents(Prefix(ESW, "primary"))

    val sequenceManager = TestSetup.startSequenceManager()

    // verify that sequencer is not present
    intercept[Exception](resolveHTTPLocation(Prefix(ESW, IRIS_DARKNIGHT), Sequencer))

    val response = sequenceManager.startSequencer(ESW, IRIS_DARKNIGHT).futureValue

    // ESW-176 Verify that start sequencer return Started response with component id for master sequencer
    response should ===(StartSequencerResponse.Started(ComponentId(Prefix(ESW, IRIS_DARKNIGHT), Sequencer)))

    // verify that sequencer is started
    resolveHTTPLocation(Prefix(ESW, IRIS_DARKNIGHT), Sequencer)

    // ESW-326 Verify that shutdown sequencer returns Success
    val shutdownResponse = sequenceManager.shutdownSequencer(ESW, IRIS_DARKNIGHT).futureValue
    shutdownResponse should ===(ShutdownSequencerResponse.Success)

    // verify that sequencer are shut down
    intercept[Exception](resolveHTTPLocation(Prefix(ESW, IRIS_DARKNIGHT), Sequencer))
  }

  "should return loadscript error if configuration is missing for subsystem observation mode | ESW-176" in {
    TestSetup.startSequenceComponents(Prefix(ESW, "primary"))

    val sequenceManager = TestSetup.startSequenceManager()

    // verify that sequencer is not present
    intercept[Exception](resolveHTTPLocation(Prefix(ESW, "invalid_obs_mode"), Sequencer))

    val response: StartSequencerResponse = sequenceManager.startSequencer(ESW, "invalid_obs_mode").futureValue

    response shouldBe a[LoadScriptError]
    val loadScriptError: LoadScriptError = response.asInstanceOf[LoadScriptError]
    loadScriptError.msg should ===("Script configuration missing for [ESW] with [invalid_obs_mode]")
  }

  private def sequencerConnection(prefix: Prefix) = AkkaConnection(ComponentId(prefix, Sequencer))

  private def assertThatSeqCompIsAvailable(prefix: Prefix): Unit = assertSeqCompAvailability(isSeqCompAvailable = true, prefix)
  private def assertThatSeqCompIsLoadedWithScript(prefix: Prefix): Unit =
    assertSeqCompAvailability(isSeqCompAvailable = false, prefix)

  private def assertSeqCompAvailability(isSeqCompAvailable: Boolean, prefix: Prefix): Unit = {
    val seqCompStatus = new SequenceComponentImpl(resolveSequenceComponentLocation(prefix)).status.futureValue
    if (isSeqCompAvailable) seqCompStatus.response shouldBe None // assert sequence component is available
    else seqCompStatus.response.isDefined shouldBe true          // assert sequence components is busy
  }

  object TestSetup {
    private val seqCompWirings    = ArrayBuffer.empty[SequenceComponentWiring]
    private val seqManagerWirings = ArrayBuffer.empty[SequenceManagerWiring]

    // Setup Sequence components for subsystems
    def startSequenceComponents(prefixes: Prefix*): Unit =
      prefixes.foreach { prefix =>
        seqCompWirings += SequencerApp.run(SequenceComponent(prefix.subsystem, Some(prefix.componentName)))
      }

    def startSequenceManager(): SequenceManagerApi = {
      val configFilePath = Paths.get(ClassLoader.getSystemResource("smResources.conf").toURI)
      val wiring         = SequenceManagerApp.run(StartCommand(configFilePath))
      seqManagerWirings += wiring
      val smLocation = resolveAkkaLocation(sequenceManagerPrefix, Service)
      new SequenceManagerImpl(smLocation)
    }

    def cleanup(): Unit = {
      seqCompWirings.foreach(_.cswWiring.actorRuntime.shutdown(CoordinatedShutdown.JvmExitReason).futureValue)
      seqManagerWirings.foreach(_.shutdown(CoordinatedShutdown.JvmExitReason).futureValue)
      seqCompWirings.clear()
      seqManagerWirings.clear()
    }
  }
}
