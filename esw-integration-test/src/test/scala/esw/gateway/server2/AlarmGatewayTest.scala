package esw.gateway.server2

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{Done, actor}
import com.typesafe.config.ConfigFactory
import csw.alarm.client.AlarmServiceFactory
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.models.Subsystem
import csw.testkit.{AlarmTestKit, LocationTestKit}
import esw.gateway.api.clients.AlarmClient
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest
import esw.http.core.BaseTestSuite
import mscoket.impl.post.PostClient
import msocket.api.RequestClient

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AlarmGatewayTest extends BaseTestSuite with RestlessCodecs {

  implicit private val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")
  implicit val untypedActorSystem: actor.ActorSystem           = actorSystem.toUntyped
  implicit val mat: Materializer                               = ActorMaterializer()
  private val alarmTestKit                                     = AlarmTestKit()
  private val locationTestKit                                  = LocationTestKit()
  private var port: Int                                        = _
  private var gatewayWiring: GatewayWiring                     = _

  private implicit lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    locationTestKit.startLocationServer()
    alarmTestKit.startAlarmService()
    port = 6490
    gatewayWiring = new GatewayWiring(Some(port))
    gatewayWiring.httpService.registeredLazyBinding.futureValue
  }

  override protected def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
    alarmTestKit.shutdown()
    locationTestKit.shutdownLocationServer()
  }

  "AlarmApi" must {
    "set alarm severity of a given alarm | ESW-216" in {
      val postClient: RequestClient[PostRequest] = new PostClient[PostRequest](s"http://localhost:$port/post")
      val alarmClient                            = new AlarmClient(postClient)

      val config              = ConfigFactory.parseResources("alarm_key.conf")
      val alarmServiceFactory = new AlarmServiceFactory()
      val alarmAdminService   = alarmServiceFactory.makeAdminApi(locationService)
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
