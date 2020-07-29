package esw.agent.app

import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand._
import esw.agent.api.ComponentStatus.NotAvailable
import esw.agent.api.{AgentCommand, AgentStatus, Failed}
import esw.agent.app.process.{ProcessExecutor, ProcessManager}

class AgentActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger
) {

  import logger._

  private[agent] def behavior(state: AgentState): Behaviors.Receive[AgentCommand] =
    Behaviors.receive[AgentCommand] { (ctx, command) =>
      import ctx.executionContext
      command match {
        //already spawning or registered
        case cmd: SpawnCommand if state.exist(cmd.componentId) =>
          val message = "given component is already in process"
          warn(message, Map("prefix" -> cmd.componentId.prefix))
          cmd.replyTo ! Failed(message)
          Behaviors.same

        //happy path
        case cmd: SpawnCommand =>
          val processManager = new ProcessManager(locationService, processExecutor, agentSettings, logger, cmd)(ctx.system)
          processManager.spawn(ctx.self).map(cmd.replyTo ! _)
          behavior(state.add(cmd.componentId, processManager))

        case KillComponent(replyTo, componentId) =>
          state.components.get(componentId) match {
            case Some(processManager) =>
              processManager.kill.map { kr =>
                ctx.self ! ProcessExited(componentId)
                replyTo ! kr
              }
            case None =>
              val message = "given component id is not running on this agent"
              error(message, Map("prefix" -> componentId.prefix))
              replyTo ! Failed(message)
          }
          Behaviors.same

        case GetComponentStatus(replyTo, componentId) =>
          state.components.get(componentId) match {
            case Some(processManager) => replyTo ! processManager.getStatus
            case None                 => replyTo ! NotAvailable
          }
          Behaviors.same

        case GetAgentStatus(replyTo) =>
          val status = state.components.view.mapValues(_.getStatus).toMap
          replyTo ! AgentStatus(status)
          Behaviors.same

        //process has exited and child actor died
        case ProcessExited(componentId) => behavior(state.remove(componentId))
      }
    }
}

private[agent] case class AgentState(components: Map[ComponentId, ProcessManager]) {
  def add(componentId: ComponentId, processManager: ProcessManager): AgentState =
    copy(components = components + (componentId -> processManager))
  def remove(componentId: ComponentId): AgentState = copy(components = components - componentId)
  def exist(componentId: ComponentId): Boolean     = components.contains(componentId)
}

private[agent] object AgentState {
  val empty: AgentState = AgentState(Map.empty)
}
