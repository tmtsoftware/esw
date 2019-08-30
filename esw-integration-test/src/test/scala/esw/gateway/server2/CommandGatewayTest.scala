package esw.gateway.server2

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.ActorMaterializer
import com.typesafe.config.ConfigFactory
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName}
import csw.testkit.FrameworkTestKit
import esw.gateway.api.clients.CommandClient
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.api.messages.{PostRequest, WebsocketRequest}
import esw.http.core.BaseTestSuite
import esw.http.core.commons.CoordinatedShutdownReasons
import mscoket.impl.post.PostClientJvm
import mscoket.impl.ws.WebsocketClientJvm
import msocket.api.RequestClient

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

class CommandGatewayTest extends BaseTestSuite with RestlessCodecs {

  implicit private val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")
  implicit val untypedActorSystem: actor.ActorSystem           = actorSystem.toUntyped
  implicit val mat: Materializer                               = ActorMaterializer()
  private val frameworkTestKit                                 = FrameworkTestKit()
  private var port: Int                                        = _
  private var gatewayWiring: GatewayWiring                     = _

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    frameworkTestKit.start()
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

    "handle validate, oneway, submit, subscribe current state and queryFinal commands | ESW-216" in {
      val postClient: RequestClient[PostRequest] = new PostClientJvm[PostRequest](s"http://localhost:$port/post")
      val websocketClient: RequestClient[WebsocketRequest] =
        new WebsocketClientJvm[WebsocketRequest](s"ws://localhost:$port/websocket")
      val commandClient = new CommandClient(postClient, websocketClient)

      frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))

      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)
      val stateNames              = Set(StateName("stateName1"), StateName("stateName2"))
      val currentState1           = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2           = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStatesF: Future[Seq[CurrentState]] =
        commandClient.subscribeCurrentState(componentId, stateNames, None).take(2).runWith(Sink.seq)
      Thread.sleep(1000)

      //validate
      commandClient.process(componentId, command, Validate).rightValue should ===(Accepted(runId))
      //oneway
      commandClient.process(componentId, command, Oneway).rightValue should ===(Accepted(runId))
      //submit
      commandClient.process(componentId, command, Submit).rightValue should ===(Completed(runId))

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))

      //queryFinal
      commandClient.queryFinal(componentId, runId).rightValue should ===(Completed(runId))
    }
  }

}
