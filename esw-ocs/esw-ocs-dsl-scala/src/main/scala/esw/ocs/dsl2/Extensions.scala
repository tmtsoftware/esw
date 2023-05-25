package esw.ocs.dsl2

import csw.params.commands.{Command, CommandResponse, Result}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.core.generics.Parameter
import csw.params.events.{Event, ObserveEvent, SystemEvent}
import esw.ocs.dsl2.highlevel.models.{CommandError, OtherError, ScriptError}

object Extensions {
  extension (submitResponse: SubmitResponse)
    def isStarted: Boolean   = submitResponse == Started
    def isCompleted: Boolean = submitResponse == Completed
    def isFailed: Boolean    = CommandResponse.isNegative(submitResponse)

    inline def onStarted(inline block: Started => Unit): SubmitResponse =
      submitResponse match
        case x: Started => block(x)
        case _          =>
      submitResponse

    inline def onCompleted(inline block: Completed => Unit): SubmitResponse =
      submitResponse match
        case x: Completed => block(x)
        case _            =>
      submitResponse

    inline def onFailed(inline block: SubmitResponse => Unit): SubmitResponse =
      if (isFailed) then block(submitResponse)
      submitResponse

    def onFailedTerminate(): SubmitResponse =
      if (submitResponse.isFailed) throw CommandError(submitResponse)
      submitResponse

    // =========== unsafe extensions ===========

    def completed: Completed =
      submitResponse match
        case x: Completed => x
        case _            => throw CommandError(submitResponse)

    def result: Result =
      submitResponse match
        case x: Completed => x.result
        case _            => throw CommandError(submitResponse)

  // ==========================================================

  extension (error: Throwable)
    def toScriptError: ScriptError = error match {
      case x: CommandError => x
      case _               => OtherError(error.getMessage, Some(error))
    }

  extension (event: Event)
    def add[T](parameter: Parameter[T]): Event = event match
      case x: SystemEvent  => x.add(parameter)
      case x: ObserveEvent => x.add(parameter)

  extension (command: Command) def obsId: String = command.maybeObsId.get.toString
}
