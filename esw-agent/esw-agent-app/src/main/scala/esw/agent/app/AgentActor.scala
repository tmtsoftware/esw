package esw.agent.app

import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.models.ComponentId
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand._
import esw.agent.api.ComponentStatus.NotAvailable
import esw.agent.api._
import esw.agent.app.ext.FutureEitherExt.FutureEitherOps
import esw.agent.app.process.ProcessManager

import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

class AgentActor(processManager: ProcessManager)(implicit log: Logger) {
  import log._

  private[agent] def behavior(state: AgentState): Behaviors.Receive[AgentCommand] =
    Behaviors.receive[AgentCommand] { (ctx, command) =>
      import ctx.executionContext

      def swap[M](x: Option[Future[M]]): Future[Option[M]] = Future.sequence(x.toList).map(_.headOption)

      command match {
        //already spawning or registered
        case cmd: SpawnCommand if state.exist(cmd.componentId) =>
          val failed = Failed(s"Component ${cmd.componentId.fullName} is already running on this agent".tap(warn(_)))
          cmd.replyTo ! failed
          Behaviors.same

        //happy path
        case cmd: SpawnCommand =>
          val spawnRes = processManager.spawn(cmd, ctx.self)
          spawnRes
            .mapRight { process =>
              ctx.self ! UpdateComponentState(cmd.componentId, ComponentState(Some(process)))
              cmd.replyTo ! Spawned
            }
            .mapLeft(cmd.replyTo ! Failed(_))

          behavior(state.add(cmd.componentId, ComponentState(None)))

        case KillComponent(replyTo, componentId) =>
          lazy val failed = Failed(s"Component ${componentId.fullName} is not running on this agent".tap(warn(_)))
          swap(state.components.get(componentId).flatMap(_.process.map(processManager.kill)))
            .map(_.fold[KillResponse](failed)(identity))
            .map(replyTo ! _)
          Behaviors.same

        case UpdateComponentState(componentId, componentState) => behavior(state.add(componentId, componentState))

        case GetComponentStatus(replyTo, componentId) =>
          replyTo ! state.components.get(componentId).fold[ComponentStatus](NotAvailable)(_.status)
          Behaviors.same

        case GetAgentStatus(replyTo) =>
          replyTo ! AgentStatus(state.components.view.mapValues(_.status).toMap)
          Behaviors.same

        //process has exited and child actor died
        case ProcessExited(componentId) => behavior(state.remove(componentId))
      }
    }
}

private[agent] case class AgentState(components: Map[ComponentId, ComponentState]) {
  def add(componentId: ComponentId, componentState: ComponentState): AgentState =
    copy(components = components + (componentId -> componentState))
  def remove(componentId: ComponentId): AgentState = copy(components = components - componentId)
  def exist(componentId: ComponentId): Boolean     = components.contains(componentId)
}

private[agent] object AgentState {
  val empty: AgentState = AgentState(Map.empty)
}
