package esw.sm.utils

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import esw.ocs.impl.messages.SequenceComponentMsg

object RichAkkaLocation {
  implicit class RichAkkaLocation(val location: AkkaLocation) extends AnyVal {
    def toSequenceComponentRef(implicit actorSystem: ActorSystem[_]): ActorRef[SequenceComponentMsg] =
      location.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
  }
}
