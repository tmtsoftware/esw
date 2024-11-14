package esw.ocs.dsl.script.utils

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType}
import csw.prefix.models.Prefix
import esw.commons.extensions.FutureEitherExt.FutureEitherJavaOps
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts

import java.util.concurrent.CompletionStage

/**
 * A util class written to resolve the pekko Location and actor ref of a particular component from location service(in java models)
 *
 * @param locationServiceUtil - an instance of util class LocationServiceUtil
 * @param actorSystem - a Pekko ActorSystem
 */
class CommandUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[?]) {
  import actorSystem.executionContext

  /**
   * This method resolve the pekko location of a particular component from the location service
   * and returns the response in java model
   *
   * @param prefix - prefix of the component
   * @param componentType - ComponentType of the component - e.g., HCD, Sequencer etc
   * @return a [[csw.location.api.models.PekkoLocation]] as a CompletionStage value
   */
  def jResolvePekkoLocation(prefix: Prefix, componentType: ComponentType): CompletionStage[PekkoLocation] =
    locationServiceUtil
      .resolve(PekkoConnection(ComponentId(prefix, componentType)), CommonTimeouts.ResolveLocation)
      .toJava

  /**
   * This method resolve the typed actor ref of a particular component from the location service
   * and returns the response in java model
   *
   * @param prefix - prefix of the component
   * @param componentType - ComponentType of the component - e.g., HCD, Sequencer etc
   * @return a typed [[org.apache.pekko.actor.typed.ActorRef]] which understand only [[csw.command.client.messages.ComponentMessage]] as a CompletionStage value
   */
  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    jResolvePekkoLocation(prefix, componentType).thenApply { l => l.componentRef }
}
