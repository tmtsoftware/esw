package esw.sm.utils

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.prefix.models.{Prefix, Subsystem}
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.Spawned
import esw.agent.client.AgentClient
import esw.ocs.api.SequenceComponentApi
import esw.ocs.impl.SequenceComponentImpl
import esw.ocs.impl.internal.LocationServiceUtil
import esw.ocs.impl.messages.SequenceComponentMsg

import scala.concurrent.Future
import scala.util.Random

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_], timeout: Timeout) {
  import actorSystem.executionContext

  private def toSequenceComponentRef(location: AkkaLocation): ActorRef[SequenceComponentMsg] = {
    location.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
  }

  private def getAgent: Future[AgentClient] = {
    locationServiceUtil
      .listBy(ESW, Machine)
      .flatMap(locations => AgentClient.make(locations.head.prefix, locationServiceUtil.locationService))
  }

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[SequenceComponentApi] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    for {
      agentClient     <- getAgent
      Spawned         <- agentClient.spawnSequenceComponent(sequenceComponentPrefix)
      seqCompLocation <- locationServiceUtil.resolveAkkaLocation(sequenceComponentPrefix, SequenceComponent)
    } yield {
      new SequenceComponentImpl(toSequenceComponentRef(seqCompLocation))
    }
  }
}
