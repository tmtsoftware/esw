package esw.gateway.server

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.params.core.models.Subsystem
import csw.testkit.scaladsl.CSWService.AlarmServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.gateway.api.clients.AlarmClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.http.core.FutureEitherExt
import mscoket.impl.post.PostClient
import org.scalatest.WordSpecLike

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AlarmGatewayTest extends ScalaTestFrameworkTestKit(AlarmServer) with WordSpecLike with FutureEitherExt with GatewayCodecs {
  import frameworkTestKit._
  import frameworkWiring.{alarmServiceFactory, locationService}

  implicit val typedSystem: ActorSystem[SpawnProtocol] = actorSystem
  private val port: Int                                = 6490
  private val gatewayWiring: GatewayWiring             = new GatewayWiring(Some(port))

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
  }

  override protected def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "AlarmApi" must {
    "set alarm severity of a given alarm | ESW-216" in {
      val postClient  = new PostClient[PostRequest](s"http://localhost:$port/post")
      val alarmClient = new AlarmClient(postClient)

      val config            = ConfigFactory.parseResources("alarm_key.conf")
      val alarmAdminService = alarmServiceFactory.makeAdminApi(locationService)
      alarmAdminService.initAlarms(config, reset = true).futureValue

      val componentName = "trombone"
      val alarmName     = "tromboneAxisHighLimitAlarm"
      val subsystemName = Subsystem.NFIRAOS
      val majorSeverity = AlarmSeverity.Major
      val alarmKey      = AlarmKey(subsystemName, componentName, alarmName)

      alarmClient.setSeverity(alarmKey, majorSeverity).rightValue should ===(Done)
    }
  }

}
