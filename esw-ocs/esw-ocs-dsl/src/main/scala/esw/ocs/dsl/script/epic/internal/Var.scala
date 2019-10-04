package esw.ocs.dsl.script.epic.internal

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Sink
import esw.ocs.dsl.script.epic.{ProgramContext, Refreshable}

import scala.concurrent.Future
import scala.reflect.ClassTag

class Var[T: ClassTag](init: T, key: String, field: String)(implicit programContext: ProgramContext, refreshable: Refreshable) {
  import programContext._

  @volatile
  private var _value = init
  def set(x: T): Unit = {
    _value = x
  }

  def :=(x: T): Unit = set(x)
  def get: T         = _value

  def pvPut(): Unit = {
    eventService.publish(key, field, get)
  }

  def pvGet(): Unit = {
    eventService.get(key).foreach { event =>
      event.params.get(field).collect {
        case value: T => setValue(value, "pvGet")
      }
    }
  }

  def pvMonitor(): KillSwitch = {
    eventService
      .subscribe(key)
      .map(_.params.get(field))
      .collect {
        case Some(x: T) => x
      }
      .mapAsync(1) { event =>
        Future.unit.flatMap { _ =>
          setValue(event, "monitor")
        }
      }
      .to(Sink.ignore)
      .run()
  }

  private def setValue(value: T, source: String): Future[Done] = {
    set(value)
    refreshable.refresh(source)
  }

  override def toString: String = _value.toString
}

object Var {
  def assign[T: ClassTag](init: T, eventKey: String, processVar: String)(
      implicit programContext: ProgramContext,
      refreshable: Refreshable
  ): Var[T] = new Var[T](init, eventKey, processVar)
}
