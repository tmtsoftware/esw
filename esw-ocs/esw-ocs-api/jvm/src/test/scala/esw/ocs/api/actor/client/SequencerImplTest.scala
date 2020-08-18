package esw.ocs.api.actor.client

import java.net.URI

import akka.actor.typed.ActorRef
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState
import esw.ocs.api.actor.messages.SequencerState.Loaded
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.{Ok, SubmitResult, Unhandled}
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import scala.concurrent.duration.DurationInt
import scala.util.Random

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

  private def randomString5            = Random.nextString(5)
  private def randomSequencerStateName = randomFrom(SequencerState.values.toList).entryName

  "getSequence | ESW-222" in {
    val getSequenceResponse = mock[Option[StepList]]
    withBehavior {
      case GetSequence(replyTo) => replyTo ! getSequenceResponse
    } check { s =>
      s.getSequence.futureValue should ===(getSequenceResponse)
    }
  }

  "isAvailable | ESW-222" in {
    withBehavior {
      case GetSequencerState(replyTo) => replyTo ! Loaded
    } check { s =>
      s.isAvailable.futureValue should ===(false)
    }
  }

  "isOnline | ESW-222" in {
    withBehavior {
      case GetSequencerState(replyTo) => replyTo ! Loaded
    } check { s =>
      s.isOnline.futureValue should ===(true)
    }
  }

  "add | ESW-222" in {
    withBehavior {
      case Add(List(`command`), replyTo) => replyTo ! Ok
    } check { s =>
      s.add(List(command)).futureValue should ===(Ok)
    }
  }

  "prepend | ESW-222" in {
    val prependResponse = Unhandled(randomSequencerStateName, randomString5)
    withBehavior {
      case Prepend(List(`command`), replyTo) => replyTo ! prependResponse
    } check { s =>
      s.prepend(List(command)).futureValue should ===(prependResponse)
    }
  }

  "replace | ESW-222" in {
    withBehavior {
      case Replace(`stepId`, List(`command`), replyTo) => replyTo ! CannotOperateOnAnInFlightOrFinishedStep
    } check { s =>
      s.replace(stepId, List(command)).futureValue should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "insertAfter | ESW-222" in {
    withBehavior {
      case InsertAfter(`stepId`, List(`command`), replyTo) => replyTo ! Ok
    } check { s =>
      s.insertAfter(stepId, List(command)).futureValue should ===(Ok)
    }
  }

  "delete | ESW-222" in {
    val deleteResponse = IdDoesNotExist(Id())
    withBehavior {
      case Delete(`stepId`, replyTo) => replyTo ! deleteResponse
    } check { s =>
      s.delete(stepId).futureValue should ===(deleteResponse)
    }
  }

  "pause | ESW-222" in {
    withBehavior {
      case Pause(replyTo) => replyTo ! CannotOperateOnAnInFlightOrFinishedStep
    } check { s =>
      s.pause.futureValue should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "resume | ESW-222" in {
    val resumeResponse = Unhandled(randomSequencerStateName, randomString5)
    withBehavior {
      case Resume(replyTo) => replyTo ! resumeResponse
    } check { s =>
      s.resume.futureValue should ===(resumeResponse)
    }
  }

  "addBreakpoint | ESW-222" in {
    val addBreakpointResponse = Unhandled(randomSequencerStateName, randomString5)
    withBehavior {
      case AddBreakpoint(`stepId`, replyTo) => replyTo ! addBreakpointResponse
    } check { s =>
      s.addBreakpoint(stepId).futureValue should ===(addBreakpointResponse)
    }
  }

  "removeBreakpoint | ESW-222" in {
    val removeBreakpointResponse = IdDoesNotExist(Id())
    withBehavior {
      case RemoveBreakpoint(`stepId`, replyTo) => replyTo ! removeBreakpointResponse
    } check { s =>
      s.removeBreakpoint(stepId).futureValue should ===(removeBreakpointResponse)
    }
  }

  "reset | ESW-222" in {
    withBehavior {
      case Reset(replyTo) => replyTo ! Ok
    } check { s =>
      s.reset().futureValue should ===(Ok)
    }
  }

  "abortSequence | ESW-222" in {
    val abortResponse = Unhandled(randomSequencerStateName, randomString5)
    withBehavior {
      case AbortSequence(replyTo) => replyTo ! abortResponse
    } check { s =>
      s.abortSequence().futureValue should ===(abortResponse)
    }
  }

  "stop | ESW-222" in {
    val stopResponse = Unhandled(randomSequencerStateName, randomString5)
    withBehavior {
      case Stop(replyTo) => replyTo ! stopResponse
    } check { s =>
      s.stop().futureValue should ===(stopResponse)
    }
  }

  // commandApi

  "loadSequence | ESW-222" in {
    withBehavior {
      case LoadSequence(`sequence`, replyTo) => replyTo ! Ok
    } check { s =>
      s.loadSequence(sequence).futureValue should ===(Ok)
    }
  }

  "startSequence | ESW-222" in {
    val startSequenceResponse = SubmitResult(Started(Id("runId1")))
    withBehavior {
      case StartSequence(replyTo) => replyTo ! startSequenceResponse
    } check { s =>
      s.startSequence().futureValue should ===(startSequenceResponse.toSubmitResponse())
    }
  }

  "submit | ESW-222" in {
    val submitSequenceResponse = SubmitResult(Started(Id(randomString5)))
    withBehavior {
      case SubmitSequenceInternal(`sequence`, replyTo) => replyTo ! submitSequenceResponse
    } check { s =>
      s.submit(sequence).futureValue should ===(submitSequenceResponse.toSubmitResponse())
    }
  }

  "submitAndWait | ESW-222" in {
    val id                 = Id(randomString5)
    val queryFinalResponse = mock[SubmitResponse]
    withBehavior {
      case SubmitSequenceInternal(`sequence`, replyTo) => replyTo ! SubmitResult(Started(id))
      case QueryFinal(`id`, replyTo)                   => replyTo ! queryFinalResponse
    } check { s =>
      s.submitAndWait(sequence).futureValue should ===(queryFinalResponse)
    }
  }

  "queryFinal | ESW-222" in {
    val queryFinalResponse = Completed(Id())
    withBehavior {
      case QueryFinal(`sequenceId`, replyTo) => replyTo ! queryFinalResponse
    } check { s =>
      s.queryFinal(sequenceId).futureValue should ===(queryFinalResponse)
    }
  }

  "diagnosticMode | ESW-143" in {
    withBehavior {
      case DiagnosticMode(`startTime`, `hint`, replyTo) => replyTo ! Ok
    } check { s =>
      s.diagnosticMode(startTime, hint).futureValue should ===(Ok)
    }
  }

  "operationsMode | ESW-143" in {
    withBehavior {
      case OperationsMode(replyTo) => replyTo ! Ok
    } check { s =>
      s.operationsMode().futureValue should ===(Ok)
    }
  }

  "getSequenceComponent | ESW-255" in {
    val getSequenceComponentResponse =
      AkkaLocation(
        AkkaConnection(ComponentId(Prefix(randomSubsystem, randomString5), SequenceComponent)),
        new URI(randomString5),
        Metadata.empty
      )
    withBehavior {
      case GetSequenceComponent(replyTo) => replyTo ! getSequenceComponentResponse
    } check { s =>
      s.getSequenceComponent.futureValue should ===(getSequenceComponentResponse)
    }
  }
}
