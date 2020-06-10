package esw.agent.app

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand._
import esw.agent.api.ComponentStatus.NotAvailable
import esw.agent.api.{AgentCommand, AgentStatus, Failed}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.ProcessActorMessage.{Die, GetStatus, SpawnComponent}
import esw.agent.app.process.{ProcessActor, ProcessActorMessage, ProcessExecutor}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AgentActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger
) {

  import logger._

  private[agent] def behavior(state: AgentState): Behaviors.Receive[AgentCommand] =
    Behaviors.receive[AgentCommand] { (ctx, command) =>
      command match {
        //already spawning or registered
        case cmd: SpawnCommand if state.exist(cmd.componentId) =>
          val message = "given component is already in process"
          warn(message, Map("prefix" -> cmd.componentId.prefix))
          cmd.replyTo ! Failed(message)
          Behaviors.same

        //happy path
        case cmd: SpawnCommand =>
          val initBehaviour   = new ProcessActor(locationService, processExecutor, agentSettings, logger, cmd).init
          val processActorRef = ctx.spawn(initBehaviour, cmd.componentId.prefix.toString.toLowerCase)
          ctx.watchWith(processActorRef, ProcessExited(cmd.componentId))
          processActorRef ! SpawnComponent
          behavior(state.add(cmd.componentId, processActorRef))

        case KillComponent(replyTo, componentId) =>
          state.components.get(componentId) match {
            case Some(processActor) => processActor ! Die(replyTo)
            case None =>
              val message = "given component id is not running on this agent"
              error(message, Map("prefix" -> componentId.prefix))
              replyTo ! Failed(message)
          }
          Behaviors.same

        case GetComponentStatus(replyTo, componentId) =>
          state.components.get(componentId) match {
            case Some(childRef) => childRef ! GetStatus(replyTo)
            case None           => replyTo ! NotAvailable
          }
          Behaviors.same

        case GetAgentStatus(replyTo) =>
          import ctx.executionContext
          implicit val sch: Scheduler   = ctx.system.scheduler
          implicit val timeout: Timeout = Timeout(1.second)

          val statusF = Future.traverse(state.components.toList) {
            case (id, ref) => (ref ? GetStatus).map(status => (id, status))
          }
          statusF.foreach(m => replyTo ! AgentStatus(m.toMap))
          Behaviors.same

        //process has exited and child actor died
        case ProcessExited(componentId) => behavior(state.remove(componentId))
      }
    }
}

object AgentActor {
  private[agent] case class AgentState(components: Map[ComponentId, ActorRef[ProcessActorMessage]]) {
    def add(componentId: ComponentId, actorRef: ActorRef[ProcessActorMessage]): AgentState =
      copy(components = components + (componentId -> actorRef))
    def remove(componentId: ComponentId): AgentState = copy(components = components - componentId)
    def exist(componentId: ComponentId): Boolean     = components.contains(componentId)
  }

  private[agent] object AgentState {
    val empty: AgentState = AgentState(Map.empty)
  }
}
