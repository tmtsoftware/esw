package esw.ocs.impl.internal

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.{Prefix, Subsystem}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.Stop

import scala.concurrent.Future
import scala.util.Random
import scala.util.control.NonFatal

class SequenceComponentRegistration(
    subsystem: Subsystem,
    name: Option[String],
    _locationService: LocationService,
    sequenceComponentFactory: String => Future[ActorRef[SequenceComponentMsg]]
)(
    implicit override val actorSystem: ActorSystem[SpawnProtocol.Command]
) extends LocationServiceUtil(_locationService) {

  def registerSequenceComponent(retryCount: Int): Future[Either[ScriptError, AkkaLocation]] = name match {
    case Some(_) =>
      // Don't retry if subsystem and name is provided
      registerWithRetry(retryCount = 0)
    case None => registerWithRetry(retryCount)
  }

  private def registerWithRetry(retryCount: Int): Future[Either[ScriptError, AkkaLocation]] =
    registration().flatMap { akkaRegistration =>
      register(
        akkaRegistration,
        onFailure = {
          case OtherLocationIsRegistered(_) if retryCount > 0 =>
            //kill actor ref if registration fails. Retry attempt will create new actor ref
            akkaRegistration.actorRefURI.toActorRef.unsafeUpcast[SequenceComponentMsg] ! Stop
            registerWithRetry(retryCount - 1)
          case NonFatal(e) => Future.successful(Left(ScriptError(e.getMessage)))
        }
      )
    }

  private def registration(): Future[AkkaRegistration] = {
    val sequenceComponentName = (subsystem, name) match {
      case (s, Some(n)) => s"$s.$n"
      case (s, None)    => s"${s}.${s}_${Random.between(1, 100)}"
    }
    sequenceComponentFactory(sequenceComponentName).map { actorRef =>
      AkkaRegistration(
        AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent)),
        Prefix(sequenceComponentName),
        actorRef.toURI
      )
    }
  }
}
