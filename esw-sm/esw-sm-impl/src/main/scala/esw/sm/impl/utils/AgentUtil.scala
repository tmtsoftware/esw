package esw.sm.impl.utils

import akka.actor.typed.{ActorSystem, Scheduler}
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api._
import esw.agent.client.AgentClient
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.SpawnSequenceComponentResponse.SpawnSequenceComponentFailed
import esw.sm.api.protocol.{ProvisionResponse, SpawnSequenceComponentResponse}
import esw.sm.impl.config.ProvisionConfig

import scala.concurrent.Future

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  // todo: remove the logic to resolving seq comp after spawning
  def spawnSequenceComponentOn(
      machine: Prefix,
      seqCompName: String
  ): Future[Either[SpawnSequenceComponentResponse.Failure, SequenceComponentApi]] =
    getAgent(machine).flatMapE(spawnSeqComp(_, Prefix(machine.subsystem, seqCompName)))

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] =
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .mapLeft(e => LocationServiceError(e.msg))
      .flatMapE(provisionOn(_, provisionConfig))
      .mapToAdt(spawnResToProvisionRes, identity)

  def getSequenceComponentsRunningOn(agents: List[AkkaLocation]): Future[Map[ComponentId, List[ComponentId]]] =
    parallel(agents)(agent =>
      makeAgentClient(agent).getAgentStatus
        .map(filterRunningSeqComps)
        .map(seqComps => agent.connection.componentId -> seqComps)
    )

  private def provisionOn(machines: List[AkkaLocation], provisionConfig: ProvisionConfig) = {
    val subsystemMappedMachines  = machines.groupBy(_.prefix.subsystem)
    val subsystemsWithoutMachine = provisionConfig.config.keySet.diff(subsystemMappedMachines.keySet)

    if (subsystemsWithoutMachine.isEmpty) spawnSeqCompsOn(subsystemMappedMachines, provisionConfig).map(Right(_))
    else Future.successful(Left(ProvisionResponse.NoMachineFoundForSubsystems(subsystemsWithoutMachine)))
  }

  private def spawnSeqCompsOn(subsystemMappedMachines: Map[Subsystem, List[AkkaLocation]], provisionConfig: ProvisionConfig) = {

    val spawnResponses = Future.traverse(provisionConfig.config.toList) { subsystemConfig =>
      val (subsystem, noOfSeqComp) = subsystemConfig
      val neededSeqComps           = configToSeqComps(subsystem, noOfSeqComp)
      val subsystemMachines        = subsystemMappedMachines(subsystem)

      // round robin distribution of components on machines
      val seqCompToMachineMapping = neededSeqComps.zip(cycle(subsystemMachines: _*))
      Future.traverse(seqCompToMachineMapping) { prefixToAkkaLocation =>
        val (seqCompPrefix, agent) = prefixToAkkaLocation
        spawnOn(agent, seqCompPrefix)
      }
    }
    spawnResponses.map(_.flatten)
  }

  private def spawnResToProvisionRes(responses: List[SpawnResponse]): ProvisionResponse = {
    val failedResponses = responses.collect { case Failed(msg) => SpawnSequenceComponentFailed(msg) }

    if (failedResponses.isEmpty) ProvisionResponse.Success
    else ProvisionResponse.SpawningSequenceComponentsFailed(failedResponses)
  }

  private def configToSeqComps(subsystem: Subsystem, noOfSeqComps: Int) =
    (1 to noOfSeqComps).map(i => Prefix(subsystem, s"${subsystem}_$i"))

  private def spawnOn(location: AkkaLocation, prefix: Prefix) = makeAgentClient(location).spawnSequenceComponent(prefix)

  private def spawnSeqComp(agentClient: AgentClient, seqCompPrefix: Prefix) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix)
      .flatMap {
        case Spawned     => resolveSeqComp(seqCompPrefix)
        case Failed(msg) => Future.successful(Left(SpawnSequenceComponentFailed(msg)))
      }

  private[utils] def getAgent(prefix: Prefix): Future[Either[LocationServiceError, AgentClient]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, Machine)))
      .mapRight(location => makeAgentClient(location))
      .mapLeft(error => LocationServiceError(error.msg))

  private[utils] def makeAgentClient(loc: AkkaLocation): AgentClient = {
    implicit val sch: Scheduler = actorSystem.scheduler
    new AgentClient(loc)
  }

  private def resolveSeqComp(seqCompPrefix: Prefix) =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), within = Timeouts.DefaultResolveLocationDuration)
      .mapRight(loc => new SequenceComponentImpl(loc))
      .mapLeft(e => LocationServiceError(e.msg))

  private def filterRunningSeqComps(agentStatus: AgentStatus): List[ComponentId] =
    agentStatus.componentStatus
      .filter { componentIdStatus =>
        val (componentId, componentStatus) = componentIdStatus
        componentId.componentType == SequenceComponent && componentStatus == ComponentStatus.Running
      }
      .keys
      .toList

  private def parallel[T, T1, T2](i: List[T])(f: T => Future[(T1, T2)]): Future[Map[T1, T2]] = Future.traverse(i)(f).map(_.toMap)

  private def cycle[T](elems: T*): LazyList[T] = LazyList(elems: _*) #::: cycle(elems: _*)
}
