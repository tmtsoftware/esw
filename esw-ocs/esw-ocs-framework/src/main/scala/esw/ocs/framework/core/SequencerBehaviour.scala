//package esw.ocs.framework.core
//
//import akka.Done
//import akka.actor.typed.scaladsl.Behaviors
//import akka.actor.typed.{ActorRef, Behavior}
//import csw.command.client.messages.CommandResponseManagerMessage
//import csw.command.client.messages.CommandResponseManagerMessage.{AddOrUpdateCommand, AddSubCommand, UpdateSubCommand}
//import csw.params.commands.CommandResponse
//import csw.params.commands.CommandResponse.SubmitResponse
//import csw.params.core.models.Id
//import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight}
//import esw.ocs.framework.api.models.messages.SequencerMsg._
//import esw.ocs.framework.api.models.messages.{SequencerMsg, StepListActionResponse, StepListError}
//import esw.ocs.framework.api.models.{Sequence, Step, StepList, StepListResult}
//
//object SequencerBehaviour {
//  def behavior(crmRef: ActorRef[CommandResponseManagerMessage]): Behavior[SequencerMsg] =
//    Behaviors.setup { ctx =>
//      val crmMapper: ActorRef[SubmitResponse]    = ctx.messageAdapter(rsp ⇒ Update(rsp))
//      var stepList: StepList                     = StepList.empty
//      var seqId: Option[Id]                      = None
//      val emptyChildId                           = Id("empty-child")
//      var latestResponse: Option[SubmitResponse] = None
//
//      var seqResponseRefOpt: Option[ActorRef[Either[DuplicateIdsFound.type, SubmitResponse]]] = None
//      var stepRefOpt: Option[ActorRef[Step]]                                                  = None
//      var readyToExecuteNextRefOpt: Option[ActorRef[Done]]                                    = None
//
//      def isSequenceFinished: Boolean =
//        stepList.isFinished || latestResponse.exists(_.runId == stepList.runId)
//
//      def clearIfSequenceFinished(): Unit = {
//        if (isSequenceFinished) {
//          // fixme
//          val sequenceResponse = CommandResponse.withRunId(stepList.runId, latestResponse.orNull) //whether this will be called with None latestresponse ever??
//          crmRef ! UpdateSubCommand(CommandResponse.withRunId(emptyChildId, sequenceResponse))
//          seqResponseRefOpt.foreach(_ ! Right(sequenceResponse))
//          readyToExecuteNextRefOpt.foreach(readyToExecuteNext)
//          stepList = StepList.empty
//          latestResponse = None
//          readyToExecuteNextRefOpt = None
//        }
//      }
//
//      def processSequence(sequence: Sequence, replyTo: ActorRef[Either[ProcessSequenceError, SubmitResponse]]): Unit =
//        if (stepList.isFinished)
//          StepList(sequence)
//            .map { sl ⇒
//              val runId = sequence.runId
//              stepList = sl
//              seqId = Some(runId)
//              seqResponseRefOpt = Some(replyTo)
//
//              crmRef ! AddOrUpdateCommand(CommandResponse.Started(runId))
//              crmRef ! CommandResponseManagerMessage.Subscribe(runId, crmMapper)
//              //fixme
//              crmRef ! AddSubCommand(runId, emptyChildId)
//            }
//            .left
//            .map(error ⇒ replyTo ! Left(error))
//        else replyTo ! Left(ExistingSequenceIsInProcess)
//
//      def readyToExecuteNext(replyTo: ActorRef[Done]): Unit =
//        if (!stepList.isInFlight) replyTo ! Done
//        else readyToExecuteNextRefOpt = Some(replyTo)
//
//      def updateAndSendResponse[T <: StepListError](stepListResult: StepListResult[T], replyTo: ActorRef[T]): Unit = {
//        stepList = stepListResult.stepList
//        clearIfSequenceFinished()
//        replyTo ! stepListResult.response
//      }
//
//      def update(submitResponse: SubmitResponse): Unit = {
//        crmRef ! UpdateSubCommand(CommandResponse.withRunId(submitResponse.runId, submitResponse))
//
//        // fixme: handle errors
//        val stepListResult = stepList.updateStatus(submitResponse.runId, Finished)
//        stepList = stepListResult.stepList
//        latestResponse = Some(submitResponse)
//        clearIfSequenceFinished()
//        readyToExecuteNextRefOpt.foreach(readyToExecuteNext)
//      }
//
//      def pullNext(replyTo: ActorRef[Step]): Unit = stepList.nextExecutable match {
//        case Some(step) => setInFlight(replyTo, step)
//        case None       => stepRefOpt = Some(replyTo)
//      }
//
//      def setInFlight(replyTo: ActorRef[Step], step: Step): Unit = {
//        val stepListResult = stepList.updateStatus(step.id, InFlight)
//        stepListResult.response match {
//          case StepListActionResponse.Updated(step) ⇒
//            val stepRunId = step.id
//            crmRef ! AddSubCommand(stepList.runId, stepRunId)
//            crmRef ! AddOrUpdateCommand(CommandResponse.Started(stepRunId))
//            crmRef ! CommandResponseManagerMessage.Subscribe(stepRunId, crmMapper)
//            replyTo ! step
//          case StepListActionResponse.NotAllowedOnFinishedSeq ⇒
//          case StepListActionResponse.IdDoesNotExist(id)      ⇒
//          case StepListActionResponse.UpdateFailed            ⇒
//        }
//      }
//
//      def trySend(): Unit = {
//        for {
//          ref  <- stepRefOpt
//          step <- stepList.nextExecutable
//        } {
//          setInFlight(ref, step)
//          stepRefOpt = None
//        }
//      }
//
//      Behaviors.receiveMessage { msg =>
//        msg match {
//          case ProcessSequence(sequence, replyTo) ⇒ processSequence(sequence, replyTo)
//          case GetSequence(replyTo)               ⇒ replyTo ! stepList
//          case GetNext(replyTo)                   ⇒ pullNext(replyTo)
//          case MaybeNext(replyTo)                 ⇒ replyTo ! stepList.nextExecutable
//          case Update(submitResponse)             ⇒ update(submitResponse)
//          case Add(commands, replyTo)             ⇒ updateAndSendResponse(stepList.append(commands), replyTo)
//          case Pause(replyTo)                     ⇒ updateAndSendResponse(stepList.pause, replyTo)
//          case Resume(replyTo)                    ⇒ updateAndSendResponse(stepList.resume, replyTo)
//          case DiscardPending(replyTo)            ⇒ updateAndSendResponse(stepList.discardPending, replyTo)
//          case Replace(id, commands, replyTo)     ⇒ updateAndSendResponse(stepList.replace(id, commands), replyTo)
//          case Prepend(commands, replyTo)         ⇒ updateAndSendResponse(stepList.prepend(commands), replyTo)
//          case Delete(ids, replyTo)               ⇒ updateAndSendResponse(stepList.delete(ids.toSet), replyTo)
//          case InsertAfter(id, commands, replyTo) ⇒ updateAndSendResponse(stepList.insertAfter(id, commands), replyTo)
//          case AddBreakpoints(ids, replyTo)       ⇒ updateAndSendResponse(stepList.addBreakpoints(ids), replyTo)
//          case RemoveBreakpoints(ids, replyTo)    ⇒ updateAndSendResponse(stepList.removeBreakpoints(ids), replyTo)
//          case ReadyToExecuteNext(replyTo)        ⇒ readyToExecuteNext(replyTo)
//        }
//        trySend()
//        Behaviors.same
//      }
//    }
//}
