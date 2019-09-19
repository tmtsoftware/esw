package esw.dsl.script

import akka.actor.Scheduler
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer
import csw.command.client.messages.sequencer.SubmitSequenceAndWait
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Started
import csw.params.commands.Sequence
import csw.params.core.models.{Id, Prefix}
import esw.dsl.Timeouts
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.BaseTestSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.clearInvocations

import scala.concurrent.{ExecutionContext, Future}

class SequencerCommandServiceDslTest extends BaseTestSuite with SequencerCommandServiceDsl {

  val locationService: LocationService                 = mock[LocationService]
  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
  implicit val ec: ExecutionContext                    = actorSystem.executionContext
  implicit val scheduler: Scheduler                    = actorSystem.scheduler
  implicit val timeout: Timeout                        = Timeouts.DefaultTimeout

  val sequencerRef: ActorRef[SubmitSequenceAndWait] = (actorSystem ? Spawn(TestSequencer.beh, "testSequencerActor")).awaitResult

  val prefixStr    = "TCS.filter.wheel"
  val seqName      = "TCS@darknight"
  val connection   = AkkaConnection(ComponentId(seqName, ComponentType.Sequencer))
  val location     = AkkaLocation(connection, Prefix(prefixStr), sequencerRef.toURI)
  val registration = AkkaRegistration(connection, Prefix(prefixStr), sequencerRef.toURI)
  val sequence     = Sequence(Id(), Seq.empty)

  override def afterEach(): Unit = {
    clearInvocations(locationService)
  }

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  "submitSequence" must {
    "submit sequence to given sequencer | ESW-195, ESW-145, ESW-220" in {
      val registrationResult = mock[RegistrationResult]
      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))

      new LocationServiceUtil(locationService).register(registration).awaitResult

      val eventualResponse = submitSequence(location, sequence)
      eventualResponse.futureValue shouldBe Started(sequence.runId)
    }

  }
}

object TestSequencer {
  val beh: Behaviors.Receive[sequencer.SubmitSequenceAndWait] = Behaviors.receiveMessage[sequencer.SubmitSequenceAndWait] {
    case SubmitSequenceAndWait(sequence, replyTo) =>
      replyTo ! Started(sequence.runId)
      Behaviors.same
    case _ => Behaviors.same
  }
}
