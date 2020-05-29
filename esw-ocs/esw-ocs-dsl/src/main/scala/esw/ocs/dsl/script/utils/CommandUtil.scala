package esw.ocs.dsl.script.utils

import java.util.concurrent.CompletionStage

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.Prefix
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil

class CommandUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def jResolveAkkaLocation(prefix: Prefix, componentType: ComponentType): CompletionStage[AkkaLocation] =
    locationServiceUtil.resolve(AkkaConnection(ComponentId(prefix, componentType)), Timeouts.DefaultTimeout).toJava

  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    jResolveAkkaLocation(prefix, componentType).thenApply { l => l.componentRef }
}
