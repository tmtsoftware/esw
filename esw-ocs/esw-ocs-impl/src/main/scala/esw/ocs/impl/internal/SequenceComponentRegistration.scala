package esw.ocs.impl.internal

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.Stop
import esw.ocs.api.protocol.ScriptError.LocationServiceError

import scala.concurrent.Future
import scala.util.Random
import scala.util.control.NonFatal

class SequenceComponentRegistration(
    subsystem: Subsystem,
    name: Option[String],
    _locationService: LocationService,
    sequenceComponentFactory: Prefix => Future[ActorRef[SequenceComponentMsg]]
)(implicit
    override val actorSystem: ActorSystem[SpawnProtocol.Command]
) extends LocationServiceUtil(_locationService) {

  def registerSequenceComponent(retryCount: Int): Future[Either[LocationServiceError, AkkaLocation]] =
    name match {
      case Some(_) =>
        // Don't retry if subsystem and name is provided
        registerWithRetry(retryCount = 0)
      case None => registerWithRetry(retryCount)
    }

  private def registerWithRetry(retryCount: Int): Future[Either[LocationServiceError, AkkaLocation]] =
    registration().flatMap { akkaRegistration =>
      register(
        akkaRegistration,
        onFailure = {
          case OtherLocationIsRegistered(_) if retryCount > 0 =>
            //kill actor ref if registration fails. Retry attempt will create new actor ref
            akkaRegistration.actorRefURI.toActorRef.unsafeUpcast[SequenceComponentMsg] ! Stop
            registerWithRetry(retryCount - 1)
          case NonFatal(e) => Future.successful(Left(LocationServiceError(e.getMessage)))
        }
      )
    }

  private def registration(): Future[AkkaRegistration] = {
    val sequenceComponentPrefix = (subsystem, name) match {
      case (s, Some(n)) => Prefix(s, n)
      case (s, None)    => Prefix(s, s"${s}_${Random.between(1, 100)}")
    }
    sequenceComponentFactory(sequenceComponentPrefix).map { actorRef =>
      AkkaRegistration(AkkaConnection(ComponentId(sequenceComponentPrefix, ComponentType.SequenceComponent)), actorRef.toURI)
    }
  }
}
