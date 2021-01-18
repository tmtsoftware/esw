package esw.gateway.server
import java.nio.file.Paths
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, ComponentType}
import csw.network.utils.SocketUtils
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, ObsId}
import csw.prefix.models.Prefix
import esw.gateway.server.utils.Resolver
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.utils.BaseTestSuite

import scala.concurrent.{Await, Future}

trait GatewaySetup extends BaseTestSuite {
  val mockResolver: Resolver                 = mock[Resolver]
  val mockCommandService: CommandService     = mock[CommandService]
  val componentIdCommandService: ComponentId = ComponentId(Prefix("IRIS.filter.wheel"), Assembly)

  val irisUserLevelCommand: Setup =
    Setup(Prefix("CSW.ncc.trombone"), CommandName("startExposure"), Some(ObsId("2020A-001-123")))
  val irisAdminLevelCommand: Setup =
    Setup(Prefix("CSW.ncc.trombone"), CommandName("upgradeFirmware"), Some(ObsId("2020A-001-123")))

  val irisCommandNotInConf: Setup = Setup(Prefix("CSW.ncc.trombone"), CommandName("stopExposure"), Some(ObsId("2020A-001-123")))
  val irisEngLevelCommand: Setup  = Setup(Prefix("CSW.ncc.trombone"), CommandName("setVoltage"), Some(ObsId("2020A-001-123")))

  val mockSequencerCommandService: SequencerApi = mock[SequencerApi]
  val componentIdSequencer: ComponentId         = ComponentId(Prefix("IRIS.MoonNight"), ComponentType.Sequencer)

  val sequence: Sequence = Sequence(
    Setup(Prefix("CSW.ncc.trombone"), CommandName("startExposure"), Some(ObsId("2020A-001-123")))
  )

  when(mockResolver.commandService(componentIdCommandService)).thenReturn(Future.successful(mockCommandService))
  when(mockCommandService.submit(irisUserLevelCommand)).thenReturn(Future.successful(Started(Id("4321"))))
  when(mockCommandService.submit(irisEngLevelCommand)).thenReturn(Future.successful(Started(Id("1234"))))
  when(mockCommandService.submit(irisAdminLevelCommand)).thenReturn(Future.successful(Started(Id("9876"))))
  when(mockCommandService.submit(irisCommandNotInConf)).thenReturn(Future.successful(Started(Id("3453"))))

  when(mockResolver.sequencerCommandService(componentIdSequencer)).thenReturn(Future.successful(mockSequencerCommandService))
  when(mockSequencerCommandService.submit(sequence)).thenReturn(Future.successful(Started(Id("5678"))))

  def startGateway(): GatewayWiring = {
    val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
    val gatewayWiring = new GatewayWiring(Some(SocketUtils.getFreePort), local = true, commandRolesPath) {
      override val resolver: Resolver = mockResolver
    }
    Await.result(gatewayWiring.httpService.startAndRegisterServer(), defaultTimeout)
    gatewayWiring
  }

}
