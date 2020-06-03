package esw.agent.app.process

import akka.actor.typed.ActorRef
import esw.agent.api.{ComponentStatus, KillResponse}

private[agent] sealed trait ProcessActorMessage
private[agent] object ProcessActorMessage {
  case object SpawnComponent                               extends ProcessActorMessage
  case class Die(replyTo: ActorRef[KillResponse])          extends ProcessActorMessage
  case object AlreadyRegistered                            extends ProcessActorMessage
  case object RunCommand                                   extends ProcessActorMessage
  case object RegistrationSuccess                          extends ProcessActorMessage
  case object RegistrationFailed                           extends ProcessActorMessage
  case object Stop                                         extends ProcessActorMessage
  case class ProcessExited(exitCode: Long)                 extends ProcessActorMessage
  case class GetStatus(replyTo: ActorRef[ComponentStatus]) extends ProcessActorMessage
  case class LocationServiceError(exception: Throwable)    extends ProcessActorMessage
}
