package esw.gateway.server

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import com.typesafe.config.ConfigFactory
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.params.core.models.Subsystem
import csw.testkit.scaladsl.CSWService.AlarmServer
import esw.gateway.api.clients.AlarmClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{GatewayException, PostRequest}
import esw.ocs.testkit.EswTestKit
import msocket.impl.Encoding.JsonText
import msocket.impl.post.HttpPostTransport

class AlarmGatewayTest extends EswTestKit(AlarmServer) with GatewayCodecs {
  import frameworkTestKit.frameworkWiring.alarmServiceFactory

  private val port: Int                    = 6490
  private val gatewayWiring: GatewayWiring = new GatewayWiring(Some(port))

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
  }

  override def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "AlarmApi" must {
    "set alarm severity of a given alarm | ESW-216, ESW-86, ESW-193, ESW-233" in {
      val postClient =
        new HttpPostTransport[PostRequest, GatewayException](s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val alarmClient = new AlarmClient(postClient)

      val config            = ConfigFactory.parseResources("alarm_key.conf")
      val alarmAdminService = alarmServiceFactory.makeAdminApi(locationService)
      alarmAdminService.initAlarms(config, reset = true).futureValue

      val componentName = "trombone"
      val alarmName     = "tromboneAxisHighLimitAlarm"
      val subsystemName = Subsystem.NFIRAOS
      val majorSeverity = AlarmSeverity.Major
      val alarmKey      = AlarmKey(subsystemName, componentName, alarmName)

      alarmClient.setSeverity(alarmKey, majorSeverity).futureValue should ===(Done)
    }
  }
}
