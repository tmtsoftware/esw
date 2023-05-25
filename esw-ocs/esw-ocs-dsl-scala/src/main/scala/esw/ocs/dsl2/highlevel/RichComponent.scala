package esw.ocs.dsl2.highlevel

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.{ComponentMessage, DiagnosticDataMessage, RunningMessage}
import csw.command.client.models.framework.{LockingResponse, ToComponentLifecycleMessage}
import csw.location.api.models.ComponentType
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.dsl.script.utils.{CommandUtil, LockUnlockUtil}
import esw.ocs.dsl2.Extensions.*
import msocket.api.Subscription

import java.util.concurrent.TimeUnit
import scala.async.Async.*
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.FutureConverters.CompletionStageOps
import scala.jdk.FutureConverters.FutureOps
import scala.jdk.DurationConverters.ScalaDurationOps

class RichComponent(
    prefix: Prefix,
    componentType: ComponentType,
    lockUnlockUtil: LockUnlockUtil,
    commandUtil: CommandUtil,
    defaultTimeout: FiniteDuration
)(using ActorSystem[_], ExecutionContext) {

  inline def validate(command: ControlCommand): ValidateResponse = await(commandService().validate(command))

  inline def oneway(command: ControlCommand): OnewayResponse = await(commandService().oneway(command))

  inline def submit(command: ControlCommand, resumeOnError: Boolean = false): SubmitResponse =
    actionOnResponse(resumeOnError) {
      await(commandService().submit(command))
    }

  inline def query(commandRunId: Id, resumeOnError: Boolean = false): SubmitResponse =
    actionOnResponse(resumeOnError) {
      await(commandService().query(commandRunId))
    }

  inline def queryFinal(
      commandRunId: Id,
      timeout: FiniteDuration = defaultTimeout,
      resumeOnError: Boolean = false
  ): SubmitResponse =
    actionOnResponse(resumeOnError) {
      await(commandService().queryFinal(commandRunId)(using Timeout(timeout)))
    }

  inline def submitAndWait(
      command: ControlCommand,
      timeout: FiniteDuration = defaultTimeout,
      resumeOnError: Boolean = false
  ): SubmitResponse =
    actionOnResponse(resumeOnError) {
      await(commandService().submitAndWait(command)(using Timeout(timeout)))
    }

  inline def subscribeCurrentState(stateNames: StateName*)(inline callback: CurrentState => Unit): Subscription =
    commandService().subscribeCurrentState(stateNames.toSet, e => async(callback(e)))

  inline def diagnosticMode(startTime: UTCTime, hint: String): Unit =
    componentRef().tell(DiagnosticDataMessage.DiagnosticMode(startTime, hint))

  inline def operationsMode(): Unit = componentRef().tell(DiagnosticDataMessage.OperationsMode)

  inline def goOnline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.GoOnline))

  inline def goOffline(): Unit = componentRef().tell(RunningMessage.Lifecycle(ToComponentLifecycleMessage.GoOffline))

  inline def lock(
      leaseDuration: FiniteDuration,
      inline onLockAboutToExpire: () => Unit = () => (),
      inline onLockExpired: () => Unit = () => ()
  ): LockingResponse =
    await(
      lockUnlockUtil
        .lock(componentRef(), leaseDuration.toJava)(
          () => async({ onLockAboutToExpire(); null }).asJava,
          () => async({ onLockExpired(); null }).asJava
        )
        .asScala
    )

  inline def unlock(): LockingResponse = await(lockUnlockUtil.unlock(componentRef()).asScala)

  private inline def actionOnResponse(resumeOnError: Boolean = false)(inline block: => SubmitResponse): SubmitResponse =
    if (!resumeOnError) block.onFailedTerminate()
    else block

  private inline def commandService(): CommandService =
    CommandServiceFactory.make(await(commandUtil.jResolveAkkaLocation(prefix, componentType).asScala))

  private inline def componentRef(): ActorRef[ComponentMessage] =
    await(commandUtil.jResolveComponentRef(prefix, componentType).asScala)

}
