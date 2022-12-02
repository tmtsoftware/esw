package csw.framework

import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.framework.internal.wiring.{CswFrameworkSystem, FrameworkWiring}
import csw.framework.models.CswContext
import csw.location.api.models.ComponentType
import csw.prefix.models.Prefix
import esw.commons.extensions.FutureExt.FutureOps

class CswWiring {
  final lazy val wiring = new FrameworkWiring
  import wiring._

  private implicit lazy val cswFrameworkSystem: CswFrameworkSystem = new CswFrameworkSystem(actorSystem)

  final lazy val cswContext: CswContext =
    CswContext
      .make(
        locationService,
        eventServiceFactory,
        alarmServiceFactory,
        // dummy component info, it is not used by esw-esw.shell
        ComponentInfo(
          Prefix("csw.esw.shell"),
          ComponentType.Service,
          "",
          LocationServiceUsage.DoNotRegister
        )
      )
      .await()
}
