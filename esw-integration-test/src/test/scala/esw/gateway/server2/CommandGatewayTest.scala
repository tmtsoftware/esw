package esw.gateway.server2

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.testkit.FrameworkTestKit
import esw.gateway.api.clients.CommandClient
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.CommandAction.Submit
import esw.gateway.api.messages.PostRequest
import esw.http.core.BaseTestSuite
import esw.http.core.commons.CoordinatedShutdownReasons
import mscoket.impl.PostClientJvm
import msocket.api.RequestClient

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandGatewayTest extends BaseTestSuite with RestlessCodecs {

  private val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")
  implicit val untypedActorSystem: actor.ActorSystem  = actorSystem.toUntyped
  private val frameworkTestKit                        = FrameworkTestKit()
  private var port: Int                               = _
  private var gatewayWiring: GatewayWiring            = _

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    frameworkTestKit.startAll()
  }

  override protected def afterAll(): Unit = {
    frameworkTestKit.shutdown()
    actorSystem.terminate()
  }

  override def beforeEach(): Unit = {
    port = 6490
    gatewayWiring = new GatewayWiring(Some(port))
    Await.result(gatewayWiring.httpService.registeredLazyBinding, timeout)
  }

  override def afterEach(): Unit = {
    gatewayWiring.httpService.shutdown(CoordinatedShutdownReasons.ApplicationFinishedReason)
  }

  "CommandApi" must {

    "submit a command and return SubmitResponse | ESW-216" in {
      val postClient: RequestClient[PostRequest] = new PostClientJvm[PostRequest](s"http://localhost:$port/post")
      val commandClient                          = new CommandClient(postClient, null)

      frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))

      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)

      commandClient.process(componentId, command, Submit).rightValue should ===(Started(runId))
    }
  }

}
