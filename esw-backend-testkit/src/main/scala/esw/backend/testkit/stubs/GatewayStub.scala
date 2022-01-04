package esw.backend.testkit.stubs

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.http.SecurityDirectives
import csw.backend.auth.MockedAuth
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import csw.network.utils.SocketUtils
import esw.gateway.api.{AdminApi, AlarmApi, EventApi, LoggingApi}
import esw.gateway.server.GatewayWiring
import esw.gateway.server.utils.Resolver
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.utils.LocationUtils
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.nio.file
import java.nio.file.Paths
import scala.concurrent.Future

class GatewayStub(val locationService: LocationService)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command])
    extends LocationUtils {

  private lazy val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
  private lazy val directives       = SecurityDirectives.authDisabled(actorSystem.settings.config)

  private var gatewayWiring: Option[GatewayWiring] = None
  lazy val gatewayPort: Int                        = SocketUtils.getFreePort

  //command service mocks
  private lazy val _resolver: Resolver            = mock[Resolver]
  private lazy val commandService: CommandService = new CommandServiceStubImpl(locationService, actorSystem)
  private lazy val sequencerApi: SequencerApi     = new SequencerServiceStubImpl(locationService, actorSystem)

  //alarm service mocks
  lazy val _alarmApi: AlarmApi = new AlarmStubImpl()

  //event service mocks
  lazy val _eventApi: EventApi = new EventStubImpl(actorSystem)

  lazy val _loggingApi: LoggingApi = new LoggerStubImpl()
  lazy val _adminApi: AdminApi     = new AdminStubImpl()

  private lazy val mockedAuth = new MockedAuth

  when(_resolver.commandService(any[ComponentId]())).thenReturn(Future.successful(commandService))
  when(_resolver.sequencerCommandService(any[ComponentId]())).thenReturn(Future.successful(sequencerApi))

  private def spawnGatewayWithAuthDisabled(
      path: file.Path
  )(implicit _actorSystem: ActorSystem[SpawnProtocol.Command]): GatewayWiring =
    new GatewayWiring(Some(gatewayPort), true, path) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
      override private[esw] lazy val securityDirectives                 = directives
      override lazy val alarmApi: AlarmApi                              = _alarmApi
      override lazy val adminApi: AdminApi                              = _adminApi
      override lazy val eventApi: EventApi                              = _eventApi
      override lazy val loggingApi: LoggingApi                          = _loggingApi
      override private[esw] lazy val resolver                           = _resolver
    }

  private def spawnGatewayWithAuthEnabled(
      path: file.Path
  )(implicit _actorSystem: ActorSystem[SpawnProtocol.Command]): GatewayWiring =
    new GatewayWiring(Some(gatewayPort), true, path) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
      override private[esw] lazy val commandRoles                       = mockedAuth.commandRoles
      override private[esw] lazy val securityDirectives                 = mockedAuth._securityDirectives
      override lazy val alarmApi: AlarmApi                              = _alarmApi
      override lazy val adminApi: AdminApi                              = _adminApi
      override lazy val eventApi: EventApi                              = _eventApi
      override lazy val loggingApi: LoggingApi                          = _loggingApi
      override private[esw] lazy val resolver                           = _resolver
    }

  def spawnMockGateway(authEnabled: Boolean = false, path: file.Path = commandRolesPath): GatewayWiring = {
    val wiring = if (authEnabled) spawnGatewayWithAuthEnabled(path) else spawnGatewayWithAuthDisabled(path)
    gatewayWiring = Some(wiring)
    wiring.httpService.startAndRegisterServer().futureValue
    wiring
  }

  def shutdownGateway(): Unit = gatewayWiring.foreach(_.actorRuntime.shutdown(UnknownReason).futureValue)
}
