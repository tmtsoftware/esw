package csw.framework.testkit

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.command.client.models.framework.{ComponentInfo, LocationServiceUsage}
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.internal.wiring.{CswFrameworkSystem, FrameworkWiring}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.location.api.models.{ComponentType, Connection}
import csw.prefix.models.Prefix

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait SpawnComponent {

  val wiring: FrameworkWiring

  private def spawnComponent(
      prefix: Prefix,
      componentType: ComponentType,
      behaviorFactory: ComponentBehaviorFactory,
      locationServiceUsage: LocationServiceUsage,
      connections: Set[Connection],
      initializeTimeout: FiniteDuration
  ): Future[ActorRef[ComponentMessage]] = {
    import wiring._
    import actorRuntime._

    val componentInfo =
      ComponentInfo(prefix, componentType, "", locationServiceUsage, connections, initializeTimeout)

    val richSystem = new CswFrameworkSystem(actorRuntime.actorSystem)
    async {
      val cswCtxF            = CswContext.make(locationService, eventServiceFactory, alarmServiceFactory, componentInfo)(richSystem)
      val supervisorBehavior = SupervisorBehaviorFactory.make(None, registrationFactory, behaviorFactory, await(cswCtxF))
      await(richSystem.spawnTyped(supervisorBehavior, componentInfo.prefix.toString))
    }
  }

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

}
