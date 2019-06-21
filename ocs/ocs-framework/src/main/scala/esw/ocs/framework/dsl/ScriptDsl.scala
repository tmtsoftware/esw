package esw.ocs.framework.dsl

import csw.params.commands.{SequenceCommand, Setup}
import esw.ocs.framework.dsl.internal.FunctionBuilder

import scala.concurrent.Future
import scala.reflect.ClassTag

trait ScriptDsl extends ControlDsl {
  def csw: CswServices

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, Future[Unit]] = new FunctionBuilder

  private def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => Future[Unit]): Unit = {
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)
  }

  private val commandHandler: SequenceCommand => Future[Unit] =
    commandHandlerBuilder.build { input =>
      spawn {
        println(s"unknown command=$input")
      }
    }

  private[framework] def execute(command: SequenceCommand): Future[Unit] = spawn(commandHandler(command).await)

  protected final def handleSetupCommand(name: String)(handler: Setup => Future[Unit]): Unit = handle(name)(handler)
}
