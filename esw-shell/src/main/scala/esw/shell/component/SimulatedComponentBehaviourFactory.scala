package esw.shell.component

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

// ComponentBehaviorFactory of the simulated HCDs/Assemblies
class SimulatedComponentBehaviourFactory extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new SimulatedComponentHandlers(ctx, cswCtx)
}
