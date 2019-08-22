package esw.ocs.internal

import akka.actor.Scheduler
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.LoadAndStartSequence
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Started
import csw.params.commands.Sequence
import csw.params.core.models.{Id, Prefix}
import esw.highlevel.dsl.LocationServiceDsl
import esw.ocs.api.BaseTestSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{clearInvocations, when}

import scala.concurrent.{ExecutionContext, Future}

class SequencerCommandServiceUtilsTest extends BaseTestSuite {

  private val locationService: LocationService    = mock[LocationService]
  implicit val system: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
  implicit val ec: ExecutionContext               = system.executionContext
  implicit val scheduler: Scheduler               = system.scheduler
  implicit val timeout: Timeout                   = Timeouts.DefaultTimeout

  val sequencerCommandServiceUtil                  = new SequencerCommandServiceUtils
  val sequencerRef: ActorRef[LoadAndStartSequence] = (system ? Spawn(TestSequencer.beh, "testSequencerActor")).awaitResult

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
    system.terminate()
  }

  "submitSequence" must {
    "submit sequence to given sequencer | ESW-195" in {
      val registrationResult = mock[RegistrationResult]
      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))

      val locationServiceUtils: LocationServiceDsl = new LocationServiceDsl(locationService)
      locationServiceUtils.register(registration).awaitResult

      val eventualResponse = sequencerCommandServiceUtil.submitSequence(location, sequence)
      eventualResponse.futureValue shouldBe Started(sequence.runId)
    }

  }
}

object TestSequencer {
  val beh: Behaviors.Receive[LoadAndStartSequence] = Behaviors.receiveMessage[LoadAndStartSequence] {
    case LoadAndStartSequence(sequence, replyTo) =>
      replyTo ! Started(sequence.runId)
      Behaviors.same
    case _ => Behaviors.same
  }
}
