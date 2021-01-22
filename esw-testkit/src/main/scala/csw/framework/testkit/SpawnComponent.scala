package csw.framework.testkit

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.{ComponentMessage, TopLevelActorMessage}
import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.internal.wiring.{CswFrameworkSystem, FrameworkWiring}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.api.models.{ComponentType, Connection}
import csw.prefix.models.Prefix

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContextExecutor, Future}

trait SpawnComponent {

  val wiring: FrameworkWiring

  def spawnHCD(
      prefix: Prefix,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage = LocationServiceUsage.RegisterOnly,
      connections: Set[Connection] = Set.empty,
      initializeTimeout: FiniteDuration = 10.seconds
  ): Future[ActorRef[ComponentMessage]] =
    spawnComponent(prefix, ComponentType.HCD, behaviorFactory, locationServiceUsage, connections, initializeTimeout)

  def spawnAssembly(
      prefix: Prefix,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage = LocationServiceUsage.RegisterOnly,
      connections: Set[Connection] = Set.empty,
      initializeTimeout: FiniteDuration = 10.seconds
  ): Future[ActorRef[ComponentMessage]] =
    spawnComponent(prefix, ComponentType.Assembly, behaviorFactory, locationServiceUsage, connections, initializeTimeout)

  private def spawnComponent(
      prefix: Prefix,
      componentType: ComponentType,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage,
      connections: Set[Connection],
      initializeTimeout: FiniteDuration
  ): Future[ActorRef[ComponentMessage]] = {
    implicit val ec: ExecutionContextExecutor   = wiring.actorRuntime.ec
    implicit val richSystem: CswFrameworkSystem = new CswFrameworkSystem(wiring.actorRuntime.actorSystem)
    val componentInfo =
      ComponentInfo(prefix, componentType, "", locationServiceUsage, connections, initializeTimeout)

    async {
      val cswCtx =
        await(CswContext.make(wiring.locationService, wiring.eventServiceFactory, wiring.alarmServiceFactory, componentInfo))
      val supervisorBehavior = SupervisorBehaviorFactory.make(None, wiring.registrationFactory, behaviorFactory, cswCtx)

      await(richSystem.spawnTyped(supervisorBehavior, componentInfo.prefix.toString))
    }
  }
}
