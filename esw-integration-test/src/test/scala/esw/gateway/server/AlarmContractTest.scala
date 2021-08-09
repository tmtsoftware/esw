package esw.gateway.server

import akka.Done
import com.typesafe.config.ConfigFactory
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS
import csw.testkit.scaladsl.CSWService.AlarmServer
import esw.gateway.api.clients.AlarmClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.Gateway

class AlarmContractTest extends EswTestKit(AlarmServer, Gateway) with GatewayCodecs {
  import frameworkTestKit.alarmServiceFactory

  "AlarmApi" must {
    "set alarm severity of a given alarm | ESW-216, ESW-86, ESW-193, ESW-233, CSW-83" in {
      val alarmClient = new AlarmClient(gatewayPostClient)

      val config            = ConfigFactory.parseResources("alarm_key.conf")
      val alarmAdminService = alarmServiceFactory.makeAdminApi(locationService)
      alarmAdminService.initAlarms(config, reset = true).futureValue

      val alarmName     = "tromboneAxisHighLimitAlarm"
      val majorSeverity = AlarmSeverity.Major
      val alarmKey      = AlarmKey(Prefix(NFIRAOS, "trombone"), alarmName)

      alarmClient.setSeverity(alarmKey, majorSeverity).futureValue should ===(Done)
    }
  }
}
