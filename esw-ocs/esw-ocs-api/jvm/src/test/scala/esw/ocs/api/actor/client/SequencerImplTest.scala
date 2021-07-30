package esw.ocs.api.actor.client

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.params.commands.CommandResponse.{Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.actor.messages.InternalSequencerState._
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.models.{SequencerState, StepList}
import esw.ocs.api.protocol._
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SequencerImplTest extends ActorTestSuit {

  private val askProxyTestKit: AskProxyTestKit[SequencerMsg, SequencerImpl] = new AskProxyTestKit[SequencerMsg, SequencerImpl] {
    override def make(actorRef: ActorRef[SequencerMsg]): SequencerImpl = {
      new SequencerImpl(actorRef)
    }
  }

  import askProxyTestKit._

  private implicit val timeout: Timeout = 10.seconds
  private val command                   = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val getSequenceResponse       = Some(StepList(Sequence(command)))
  private val stepId                    = getSequenceResponse.get.steps.head.id
  private val sequenceId                = Id()
  private val sequence                  = Sequence(command)
  private val startTime                 = UTCTime.now()
  private val hint                      = "engineering"

  "subscribeSequencerState should create an actor source that emits the state response | ESW-213" in {
    val sequencerStateResponse = mock[SequencerStateResponse]
    withBehavior { case SubscribeSequencerState(replyTo) =>
      replyTo ! sequencerStateResponse
    } check { sequencerImpl =>
      val resStream = sequencerImpl.subscribeSequencerState()
      Await.result(resStream.runWith(Sink.head), 3.seconds) shouldEqual sequencerStateResponse
    }
  }
  "cancelling the source should unsubscribe from the sequencer state  | ESW-213" in {
    val sequencerStateResponse    = mock[SequencerStateResponse]
    var subscriber: ActorRef[_]   = null
    var unsubscribed: ActorRef[_] = null
    withBehavior {
      case SubscribeSequencerState(replyTo) =>
        subscriber = replyTo
        replyTo ! sequencerStateResponse
      case UnsubscribeSequencerState(replyTo) =>
        unsubscribed = replyTo
    } check { sequencerImpl =>
      val (subscription, resStream) = sequencerImpl.subscribeSequencerState().preMaterialize()
      Await.result(resStream.runWith(Sink.head), 3.seconds) shouldEqual sequencerStateResponse

      subscription.cancel()
      eventually(subscriber shouldEqual unsubscribed)
    }
  }

  "getSequence | ESW-222, ESW-362" in {
    val getSequenceResponse = mock[Option[StepList]]
    withBehavior { case GetSequence(replyTo) =>
      replyTo ! getSequenceResponse
    } check { s =>
      s.getSequence.futureValue should ===(getSequenceResponse)
    }
  }

  "isAvailable return false if sequencer loaded | ESW-222, ESW-362" in {
    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Loaded
    } check { s =>
      s.isAvailable.futureValue should ===(false)
    }
  }

  "isAvailable returns true if sequencer idle | ESW-222, ESW-362" in {
    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Idle
    } check { s =>
      s.isAvailable.futureValue should ===(true)
    }
  }

  "isOnline returns true if sequencer loaded | ESW-222, ESW-362" in {
    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Loaded
    } check { s =>
      s.isOnline.futureValue should ===(true)
    }
  }

  "isOnline returns false if sequencer offline | ESW-222, ESW-362" in {
    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Offline
    } check { s =>
      s.isOnline.futureValue should ===(false)
    }
  }

  "add | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case Add(List(`command`), replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.add(List(command)).futureValue should ===(okOrUnhandledResponse)
    }
  }

  "prepend | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case Prepend(List(`command`), replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.prepend(List(command)).futureValue should ===(okOrUnhandledResponse)
    }
  }

  "replace | ESW-222, ESW-362" in {
    val genericResponse = mock[GenericResponse]
    withBehavior { case Replace(`stepId`, List(`command`), replyTo) =>
      replyTo ! genericResponse
    } check { s =>
      s.replace(stepId, List(command)).futureValue should ===(genericResponse)
    }
  }

  "insertAfter | ESW-222, ESW-362" in {
    val genericResponse = mock[GenericResponse]
    withBehavior { case InsertAfter(`stepId`, List(`command`), replyTo) =>
      replyTo ! genericResponse
    } check { s =>
      s.insertAfter(stepId, List(command)).futureValue should ===(genericResponse)
    }
  }

  "delete | ESW-222, ESW-362" in {
    val genericResponse = mock[GenericResponse]
    withBehavior { case Delete(`stepId`, replyTo) =>
      replyTo ! genericResponse
    } check { s =>
      s.delete(stepId).futureValue should ===(genericResponse)
    }
  }

  "pause | ESW-222, ESW-362" in {
    val pauseResponse = mock[PauseResponse]
    withBehavior { case Pause(replyTo) =>
      replyTo ! pauseResponse
    } check { s =>
      s.pause.futureValue should ===(pauseResponse)
    }
  }

  "resume | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case Resume(replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.resume.futureValue should ===(okOrUnhandledResponse)
    }
  }

  "addBreakpoint | ESW-222, ESW-362" in {
    val genericResponse = mock[GenericResponse]
    withBehavior { case AddBreakpoint(`stepId`, replyTo) =>
      replyTo ! genericResponse
    } check { s =>
      s.addBreakpoint(stepId).futureValue should ===(genericResponse)
    }
  }

  "removeBreakpoint | ESW-222, ESW-362" in {
    val removeBreakpointResponse = mock[RemoveBreakpointResponse]
    withBehavior { case RemoveBreakpoint(`stepId`, replyTo) =>
      replyTo ! removeBreakpointResponse
    } check { s =>
      s.removeBreakpoint(stepId).futureValue should ===(removeBreakpointResponse)
    }
  }

  "reset | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case Reset(replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.reset().futureValue should ===(okOrUnhandledResponse)
    }
  }

  "abortSequence | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case AbortSequence(replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.abortSequence().futureValue should ===(okOrUnhandledResponse)
    }
  }

  "stop | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case Stop(replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.stop().futureValue should ===(okOrUnhandledResponse)
    }
  }

  // commandApi

  "loadSequence | ESW-222, ESW-362" in {
    val okOrUnhandledResponse = mock[OkOrUnhandledResponse]
    withBehavior { case LoadSequence(`sequence`, replyTo) =>
      replyTo ! okOrUnhandledResponse
    } check { s =>
      s.loadSequence(sequence).futureValue should ===(okOrUnhandledResponse)
    }
  }

  "startSequence | ESW-222, ESW-362" in {
    val sequencerSubmitResponse = mock[SequencerSubmitResponse]
    val submitResponse          = mock[SubmitResponse]
    when(sequencerSubmitResponse.toSubmitResponse()).thenReturn(submitResponse)
    withBehavior { case StartSequence(replyTo) =>
      replyTo ! sequencerSubmitResponse
    } check { s =>
      s.startSequence().futureValue should ===(submitResponse)
    }
  }

  "submit | ESW-222, ESW-362" in {
    val sequencerSubmitResponse = mock[SequencerSubmitResponse]
    val submitResponse          = mock[SubmitResponse]
    when(sequencerSubmitResponse.toSubmitResponse()).thenReturn(submitResponse)
    withBehavior { case SubmitSequenceInternal(`sequence`, replyTo) =>
      replyTo ! sequencerSubmitResponse
    } check { s =>
      s.submit(sequence).futureValue should ===(submitResponse)
    }
  }

  "submitAndWait | ESW-222, ESW-362" in {
    val id                 = Id(randomString5())
    val queryFinalResponse = mock[SubmitResponse]
    withBehavior {
      case SubmitSequenceInternal(`sequence`, replyTo) => replyTo ! SubmitResult(Started(id))
      case QueryFinal(`id`, replyTo)                   => replyTo ! queryFinalResponse
    } check { s =>
      s.submitAndWait(sequence).futureValue should ===(queryFinalResponse)
    }
  }

  "queryFinal | ESW-222, ESW-362" in {
    val submitResponse = mock[SubmitResponse]
    withBehavior { case QueryFinal(`sequenceId`, replyTo) =>
      replyTo ! submitResponse
    } check { s =>
      s.queryFinal(sequenceId).futureValue should ===(submitResponse)
    }
  }

  "diagnosticMode | ESW-143, ESW-362" in {
    val diagnosticModeResponse = mock[DiagnosticModeResponse]
    withBehavior { case DiagnosticMode(`startTime`, `hint`, replyTo) =>
      replyTo ! diagnosticModeResponse
    } check { s =>
      s.diagnosticMode(startTime, hint).futureValue should ===(diagnosticModeResponse)
    }
  }

  "operationsMode | ESW-143, ESW-362" in {
    val operationsModeResponse = mock[OperationsModeResponse]
    withBehavior { case OperationsMode(replyTo) =>
      replyTo ! operationsModeResponse
    } check { s =>
      s.operationsMode().futureValue should ===(operationsModeResponse)
    }
  }

  "getSequenceComponent | ESW-255, ESW-362" in {
    val getSequenceComponentResponse =
      AkkaLocation(
        AkkaConnection(ComponentId(Prefix(randomSubsystem, randomString5()), SequenceComponent)),
        new URI("uri"),
        Metadata.empty
      )
    withBehavior { case GetSequenceComponent(replyTo) =>
      replyTo ! getSequenceComponentResponse
    } check { s =>
      s.getSequenceComponent.futureValue should ===(getSequenceComponentResponse)
    }
  }

  "getSequencerState should return same state for Idle, Running, Loaded, Offline state | ESW-482" in {
    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Idle
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Idle) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Running
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Running) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Loaded
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Loaded) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Offline
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Offline) }

  }

  "getSequencerState should return Processing for any intermediate sequencer state | ESW-482" in {
    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! GoingOffline
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Processing) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! GoingOnline
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Processing) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! AbortingSequence
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Processing) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Stopping
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Processing) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Submitting
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Processing) }

    withBehavior { case GetSequencerState(replyTo) =>
      replyTo ! Starting
    } check { s => s.getSequencerState.futureValue should ===(SequencerState.Processing) }
  }

}
