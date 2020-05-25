package esw.gateway.server

import java.nio.file.Paths

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, ComponentType}
import csw.network.utils.SocketUtils
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, ObsId}
import csw.prefix.models.Prefix
import esw.gateway.api.clients.ClientFactory
import esw.gateway.server.utils.Resolver
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import msocket.impl.HttpError

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

class GatewayAuthTest extends EswTestKit(AAS) {

  private val mockResolver: Resolver = mock[Resolver]

  private val mockCommandService: CommandService = mock[CommandService]
  private val componentIdCommandService          = ComponentId(Prefix("IRIS.filter.wheel"), Assembly)
  private val irisUserLevelCommand               = Setup(Prefix("CSW.ncc.trombone"), CommandName("startExposure"), Some(ObsId("obsId")))
  private val irisCommandNotInConf               = Setup(Prefix("CSW.ncc.trombone"), CommandName("stopExposure"), Some(ObsId("obsId")))
  private val irisEngLevelCommand                = Setup(Prefix("CSW.ncc.trombone"), CommandName("setVoltage"), Some(ObsId("obsId")))
  private val irisAdminLevelCommand              = Setup(Prefix("CSW.ncc.trombone"), CommandName("upgradeFirmware"), Some(ObsId("obsId")))

  private val mockSequencerCommandService = mock[SequencerApi]
  private val componentIdSequencer        = ComponentId(Prefix("IRIS.MoonNight"), ComponentType.Sequencer)
  private val sequence                    = Sequence(Setup(Prefix("CSW.ncc.trombone"), CommandName("startExposure"), Some(ObsId("obsId"))))

  private var gatewayServerWiring: GatewayWiring = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayServerWiring = startGateway()

    when(mockResolver.commandService(componentIdCommandService)).thenReturn(Future.successful(mockCommandService))
    when(mockCommandService.submit(irisUserLevelCommand)).thenReturn(Future.successful(Started(Id("4321"))))
    when(mockCommandService.submit(irisEngLevelCommand)).thenReturn(Future.successful(Started(Id("1234"))))
    when(mockCommandService.submit(irisAdminLevelCommand)).thenReturn(Future.successful(Started(Id("9876"))))
    when(mockCommandService.submit(irisCommandNotInConf)).thenReturn(Future.successful(Started(Id("3453"))))

    when(mockResolver.sequencerCommandService(componentIdSequencer)).thenReturn(Future.successful(mockSequencerCommandService))
    when(mockSequencerCommandService.submit(sequence)).thenReturn(Future.successful(Started(Id("5678"))))
  }

  override def afterAll(): Unit = {
    gatewayServerWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "Gateway" must {

    "return 200 when IRIS Command requires IRIS-eng and client has IRIS-Eng role | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserIrisEngRoles)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val submitResponse = Await.result(commandService.submit(irisEngLevelCommand), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 403 when IRIS Command requires IRIS-eng but client only has IRIS-user | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(irisEngLevelCommand), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 403 when IRIS Command requires IRIS-admin but client only has IRIS-eng | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserIrisEngRoles)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(irisAdminLevelCommand), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 403 when IRIS Command requires IRIS-admin but client only has IRIS-User | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(irisAdminLevelCommand), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 200 when IRIS Command does not have any IRIS role mentioned in conf and client has IRIS-User | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val submitResponse = Await.result(commandService.submit(irisUserLevelCommand), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 403 when IRIS Command has no TCS role mentioned in conf and client has TCS-User | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithTcsUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(irisUserLevelCommand), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 200 when IRIS Command requires APS-eng role and client has APS-eng | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithApsEngRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val submitResponse = Await.result(commandService.submit(irisUserLevelCommand), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 200 when IRIS Command not present in conf and client has IRIS-User | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val submitResponse = Await.result(commandService.submit(irisCommandNotInConf), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 403 when IRIS Command not present in conf and client has TCS-User | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithTcsUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(irisCommandNotInConf), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 401 response for protected command route with no token | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(() => None)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(irisEngLevelCommand), defaultTimeout))
      httpError.statusCode shouldBe 401
    }

    "return 200 as IRIS-user can execute any IRIS sequence | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithIrisUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val sequencer                 = clientFactory.sequencer(componentIdSequencer)

      val submitResponse = Await.result(sequencer.submit(sequence), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 403 as TCS-user cannot execute any IRIS sequence | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(tokenWithTcsUserRole)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val sequencer                 = clientFactory.sequencer(componentIdSequencer)

      val httpError = intercept[HttpError](Await.result(sequencer.submit(sequence), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 401 response for protected sequencer route with no token | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(() => None)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val sequencer                 = clientFactory.sequencer(componentIdSequencer)

      val httpError = intercept[HttpError](Await.result(sequencer.submit(sequence), defaultTimeout))
      httpError.statusCode shouldBe 401
    }
  }

  private def startGateway(): GatewayWiring = {
    val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
    val gatewayWiring = new GatewayWiring(Some(SocketUtils.getFreePort), local = true, commandRolesPath) {
      override val resolver: Resolver = mockResolver
    }
    Await.result(gatewayWiring.httpService.registeredLazyBinding, defaultTimeout)
    gatewayWiring
  }

}
