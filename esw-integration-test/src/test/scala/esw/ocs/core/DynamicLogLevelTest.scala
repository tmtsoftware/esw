package esw.ocs.core

import csw.location.api.models.AkkaLocation
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.logging.models.Level.{ERROR, FATAL, TRACE}
import csw.logging.models.LogMetadata
import csw.logging.models.codecs.LoggingCodecs
import csw.prefix.models.Subsystem.ESW
import esw.gateway.api.clients.AdminClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.Gateway

class DynamicLogLevelTest extends EswTestKit(Gateway) with LoggingCodecs with GatewayCodecs {
  private var sequencerLocation: AkkaLocation = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    LoggingSystemFactory.start("logging", "version", "localhost", actorSystem)
    sequencerLocation = spawnSequencer(ESW, ObsMode("darknight"))
  }

  "get/set log level for sequencer dynamically using gateway | ESW-183" in {
    val adminClient = new AdminClient(gatewayPostClient)
    val metadata    = adminClient.getLogMetadata(sequencerLocation.connection.componentId).futureValue
    // Get initial log levels
    val expected = LogMetadata(TRACE, ERROR, TRACE, TRACE)
    metadata should ===(expected)

    // set sequencer log level to FATAL
    adminClient.setLogLevel(sequencerLocation.connection.componentId, FATAL).futureValue

    // assert sequencer log level is changed to FATAL
    val expectedAfterModification = LogMetadata(TRACE, ERROR, TRACE, FATAL)
    val newMetadata               = adminClient.getLogMetadata(sequencerLocation.connection.componentId).futureValue

    newMetadata should ===(expectedAfterModification)
  }
}
