package esw.ocs.internal

import akka.actor.Scheduler
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.LoadAndStartSequence
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Started
import csw.params.commands.Sequence
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import org.mockito.Mockito.{clearInvocations, verify, when}

import scala.concurrent.Future

class SequencerCommandServiceUtilTest extends BaseTestSuite {

  private val locationService: LocationService         = mock[LocationService]
  implicit val typedSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
  implicit val scheduler: Scheduler                    = typedSystem.scheduler
  implicit val timeout: Timeout                        = Timeouts.DefaultTimeout

  val sequencerCommandServiceUtil                  = new SequencerCommandServiceUtil(locationService)
  val sequencerRef: ActorRef[LoadAndStartSequence] = (typedSystem ? Spawn(TestSequencer.beh, "testSequencerActor")).awaitResult

  val prefixStr  = "TCS.filter.wheel"
  val seqName    = s"$prefixStr.sequencer"
  val connection = AkkaConnection(ComponentId(seqName, ComponentType.Sequencer))
  val location   = AkkaLocation(connection, Prefix(prefixStr), sequencerRef.toURI)
  val sequence   = Sequence(Id(), Seq.empty)

  override def afterEach(): Unit = {
    clearInvocations(locationService)
  }

  override def afterAll(): Unit = {
    typedSystem.terminate()
    super.afterAll()
  }

  "submitSequence" must {
    "submit sequence to given sequencer | ESW-195" in {
      //simulates that sequencer is registered for given Connection
      when(locationService.resolve(connection, timeout.duration)).thenReturn(Future.successful(Some(location)))

      val eventualResponse = sequencerCommandServiceUtil.submitSequence(seqName, sequence)

      verify(locationService).resolve(connection, timeout.duration)
      eventualResponse.futureValue shouldBe Started(sequence.runId)
    }

    "throw exception when invalid sequencer name is provided" in {
      //simulates that no sequencer is registered for given Connection
      when(locationService.resolve(connection, timeout.duration)).thenReturn(Future.successful(None))

      intercept[IllegalArgumentException] { sequencerCommandServiceUtil.submitSequence(seqName, sequence).awaitResult }
      verify(locationService).resolve(connection, timeout.duration)
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
