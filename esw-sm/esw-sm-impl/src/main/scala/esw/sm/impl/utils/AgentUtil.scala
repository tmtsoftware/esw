package esw.sm.impl.utils

import akka.actor.typed.{ActorSystem, Scheduler}
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, SpawnResponse, Spawned}
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

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] = {
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .flatMapRight(provisionOn(_, provisionConfig))
      .mapToAdt(spawnResToProvisionRes, e => LocationServiceError(e.msg))
  }

  private def provisionOn(machines: List[AkkaLocation], provisionConfig: ProvisionConfig) = {
    // todo: handle the error case where machines not available for given subsystem
    val spawnResponses = Future.traverse(provisionConfig.config.toList) { subsystemConfig =>
      val neededSeqComps = configToSeqComps(subsystemConfig)
      val subsystemMachines =
        machines.filter(_.prefix.subsystem == subsystemConfig._1) // todo: remove filtering on every iteration
      val seqCompToMachineMapping =
        neededSeqComps.zip(cycle(subsystemMachines: _*)) // round robin distribution of components on machines

      Future.traverse(seqCompToMachineMapping)(x => spawnOn(x._2, x._1))
    }
    spawnResponses.map(_.flatten)
  }

  private def spawnResToProvisionRes(responses: List[SpawnResponse]): ProvisionResponse = {
    val failedResponses = responses.collect { case Failed(msg) => SpawnSequenceComponentFailed(msg) }

    if (failedResponses.isEmpty) ProvisionResponse.Success
    else ProvisionResponse.ProvisioningFailed(failedResponses)
  }

  private def configToSeqComps(config: (Subsystem, Int)) =
    (1 to config._2).map(i => Prefix(config._1, s"${config._1}_$i"))

  private def spawnOn(location: AkkaLocation, prefix: Prefix) = makeAgent(location).spawnSequenceComponent(prefix)

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
      .mapRight(location => makeAgent(location))
      .mapLeft(error => LocationServiceError(error.msg))

  private[utils] def makeAgent(loc: AkkaLocation): AgentClient = {
    implicit val sch: Scheduler = actorSystem.scheduler
    new AgentClient(loc)
  }

  private def resolveSeqComp(seqCompPrefix: Prefix) =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), within = Timeouts.DefaultResolveLocationDuration)
      .mapRight(loc => new SequenceComponentImpl(loc))
      .mapLeft(e => LocationServiceError(e.msg))

  private def cycle[T](elems: T*): LazyList[T] = LazyList(elems: _*) #::: cycle(elems: _*)
}
