package esw.agent.akka.app

import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.models.ComponentId
import csw.logging.api.scaladsl.Logger
import esw.agent.akka.app.process.ProcessManager
import esw.agent.akka.client.AgentCommand._
import esw.agent.akka.client.{AgentCommand, ComponentState}
import esw.agent.service.api.models.ComponentStatus.NotAvailable
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps

import scala.util.chaining.scalaUtilChainingOps

class AgentActor(processManager: ProcessManager)(implicit log: Logger) {
  import log._

  private[agent] def behavior(state: AgentState): Behaviors.Receive[AgentCommand] =
    Behaviors.receive[AgentCommand] { (ctx, command) =>
      import ctx.executionContext

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

        case KillComponent(replyTo, location) =>
          processManager.kill(location).map(replyTo ! _)
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
