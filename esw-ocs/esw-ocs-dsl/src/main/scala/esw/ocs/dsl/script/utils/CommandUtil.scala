/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script.utils

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.Prefix
import esw.commons.extensions.FutureEitherExt.FutureEitherJavaOps
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts

import java.util.concurrent.CompletionStage

/**
 * An util class written to resolve the akka Location and actor ref of a particular component from location service(in java models)
 *
 * @param locationServiceUtil - an instance of util class LocationServiceUtil
 * @param actorSystem - an Akka ActorSystem
 */
class CommandUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  /**
   * This method resolve the akka location of a particular component from the location service
   * and returns the response in java model
   *
   * @param prefix - prefix of the component
   * @param componentType - ComponentType of the component - e.g., HCD, Sequencer etc
   * @return a [[csw.location.api.models.AkkaLocation]] as a CompletionStage value
   */
  def jResolveAkkaLocation(prefix: Prefix, componentType: ComponentType): CompletionStage[AkkaLocation] =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(prefix, componentType)), CommonTimeouts.ResolveLocation)
      .toJava

  /**
   * This method resolve the typed actor ref of a particular component from the location service
   * and returns the response in java model
   *
   * @param prefix - prefix of the component
   * @param componentType - ComponentType of the component - e.g., HCD, Sequencer etc
   * @return a typed [[akka.actor.typed.ActorRef]] which understand only [[csw.command.client.messages.ComponentMessage]] as a CompletionStage value
   */
  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    jResolveAkkaLocation(prefix, componentType).thenApply { l => l.componentRef }
}
