package esw.ocs.internal

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.Prefix
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.core.SequenceComponentBehavior
import esw.ocs.syntax.FutureSyntax.FutureOps

private[ocs] class SequenceComponentWiring(name: String) {
  lazy val actorRuntime = new ActorRuntime(name)
  import actorRuntime._

  lazy val sequenceComponentRef: ActorRef[SequenceComponentMsg] =
    (typedSystem ? Spawn(SequenceComponentBehavior.behavior, name)).block

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  //fixme: should this come from conf file?
  private lazy val prefix = Prefix("sequence-component")

  def start(): RegistrationResult = {
    def addCoordinatedShutdownTask(registration: RegistrationResult): Unit = {
      coordinatedShutdown.addTask(
        CoordinatedShutdown.PhaseBeforeServiceUnbind,
        s"unregistering-${registration.location}"
      )(() => registration.unregister())
    }

    val registration =
      AkkaRegistration(AkkaConnection(ComponentId(name, ComponentType.Service)), prefix, sequenceComponentRef.toURI)

    locationService
      .register(registration)
      .map { registrationResult =>
        addCoordinatedShutdownTask(registrationResult)
        registrationResult
      }
      .block
  }

}
