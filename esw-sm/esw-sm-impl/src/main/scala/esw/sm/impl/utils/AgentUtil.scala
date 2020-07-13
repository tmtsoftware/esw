package esw.sm.impl.utils

import akka.Done
import akka.actor.typed.{ActorSystem, Scheduler}
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, SpawnResponse, Spawned}
import esw.agent.client.AgentClient
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse.ProvisioningFailed
import esw.sm.api.protocol.SpawnSequenceComponentResponse.SpawnSequenceComponentFailed
import esw.sm.api.protocol.{ProvisionResponse, SpawnSequenceComponentResponse}
import esw.sm.impl.config.ProvisionConfig

import scala.concurrent.Future

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

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

    val spawnResponses = Future.traverse(provisionConfig.config.toList) { config =>
      val neededSeqComps          = configToSeqComps(config)
      val subsystemMachines       = machines.filter(_.prefix.subsystem == config._1)
      val seqCompToMachineMapping = neededSeqComps.zip(cycle(subsystemMachines: _*))

      Future.traverse(seqCompToMachineMapping)(x => spawnOn(x._2, x._1))
    }
    spawnResponses.map(_.flatten)
  }

  private def spawnResToProvisionRes(responses: List[SpawnResponse]): ProvisionResponse = {
    val segregatedResE = responses.collect {
      case Spawned     => Left(Done)
      case Failed(msg) => Right(SpawnSequenceComponentFailed(msg))
    }
    segregatedResE.sequence.map(ProvisioningFailed).getOrElse(ProvisionResponse.Success)
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
