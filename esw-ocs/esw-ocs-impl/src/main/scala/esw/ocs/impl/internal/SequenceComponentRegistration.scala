/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.impl.internal

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.EswLocationError.{OtherLocationIsRegistered, RegistrationError}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.Stop

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class SequenceComponentRegistration(
    subsystem: Subsystem,
    name: Option[String],
    agentPrefix: Option[Prefix],
    locationService: LocationService,
    sequenceComponentFactory: Prefix => Future[ActorRef[SequenceComponentMsg]]
)(implicit actorSystem: ActorSystem[SpawnProtocol.Command]) {
  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private val locationServiceUtil           = new LocationServiceUtil(locationService)

  def registerSequenceComponent(retryCount: Int): Future[Either[RegistrationError, AkkaLocation]] =
    name match {
      // Don't retry if subsystem and name is provided
      case Some(_) => registerWithRetry(retryCount = 0)
      case None    => registerWithRetry(retryCount)
    }

  private def registerWithRetry(retryCount: Int): Future[Either[RegistrationError, AkkaLocation]] =
    registration().flatMap { akkaRegistration =>
      locationServiceUtil.register(akkaRegistration).flatMap {
        case Left(_: OtherLocationIsRegistered) if retryCount > 0 =>
          // kill actor ref if registration fails. Retry attempt will create new actor ref
          akkaRegistration.actorRefURI.toActorRef.unsafeUpcast[SequenceComponentMsg] ! Stop
          registerWithRetry(retryCount - 1)
        // Do not retry in case of other errors or 0 retry count or success response
        case response => Future.successful(response)
      }
    }

  private def registration(): Future[AkkaRegistration] = {
    val sequenceComponentPrefix = (subsystem, name) match {
      case (s, Some(n)) => Prefix(s, n)
      case (s, None)    => Prefix(s, s"${s}_${Random.between(1, 100)}")
    }
    sequenceComponentFactory(sequenceComponentPrefix).map { actorRef =>
      val metadata = agentPrefix
        .map(Metadata().withAgentPrefix(_))
        .getOrElse(Metadata.empty)
        .withPid(ProcessHandle.current().pid())

      AkkaRegistrationFactory.make(
        AkkaConnection(ComponentId(sequenceComponentPrefix, ComponentType.SequenceComponent)),
        actorRef,
        metadata
      )
    }
  }
}
