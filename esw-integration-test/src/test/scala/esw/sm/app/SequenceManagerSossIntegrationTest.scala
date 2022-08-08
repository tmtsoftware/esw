package esw.sm.app

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.events.{EventKey, EventName}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{AOESW, ESW, IRIS}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.sm.api.protocol.ConfigureResponse.Success
import esw.sm.api.protocol.{ConfigureResponse, ShutdownSequencersResponse}

import scala.concurrent.Await
import scala.concurrent.duration.*

class SequenceManagerSossIntegrationTest extends EswTestKit(EventServer) {

  override def afterEach(): Unit = {
    super.afterEach()
    TestSetup.cleanup()
  }

  "SOSS" must {
    "have ability be able to spawn sequencer hierarchy and send sequence to top level sequencer | ESW-146" in {
      val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
      val obsMode               = ObsMode("IRIS_Cal")
      val sequence              = Sequence(Setup(sequenceManagerPrefix, CommandName("command-1"), None))

      // start all needed sequence components
      TestSetup.spawnSequenceComponent(ESW, None)
      TestSetup.spawnSequenceComponent(AOESW, None)
      TestSetup.spawnSequenceComponent(IRIS, None)

      val sequenceManager = TestSetup.startSequenceManager(sequenceManagerPrefix)

      // XXXXXXX
      implicit def patienceConfig: PatienceConfig = PatienceConfig(100.seconds)

      val configureResponse = sequenceManager.configure(obsMode).futureValue

      // verify ESW sequencer is considered as top level sequencer
      configureResponse should ===(ConfigureResponse.Success(ComponentId(Prefix(ESW, obsMode.name), Sequencer)))

      val successResponse = configureResponse.asInstanceOf[Success]
      val id              = successResponse.masterSequencerComponentId

      val location = resolveHTTPLocation(id.prefix, id.componentType)

      // ESW-146 : Send Sequence to master sequencer. (TestScript5 is loaded in master sequencer)
      SequencerApiFactory.make(location).submitAndWait(sequence).futureValue shouldBe a[Completed]

      // ESW-146 : Verify command handler of the script is called
      val event = eventSubscriber.get(EventKey(Prefix(ESW, "IRIS_cal"), EventName("event-1"))).futureValue
      event.isInvalid shouldBe false

      sequenceManager.shutdownObsModeSequencers(obsMode).futureValue shouldBe a[ShutdownSequencersResponse.Success.type]
    }
  }
}
