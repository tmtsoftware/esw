package esw.services.internal

import csw.services.utils.ColoredConsole.{GREEN, RED}

import scala.util.control.NonFatal

case class ManagedService[T](
    serviceName: String,
    enable: Boolean,
    private val _start: () => T,
    private val _stop: T => Unit
) {
  private var startResult: T = _

  def start(): Unit = {
    if (enable) {
      try {
        GREEN.println(s"Starting $serviceName ...")
        startResult = _start()
        GREEN.println(s"Successfully started $serviceName.")
      }
      catch {
        case NonFatal(e) =>
          RED.println(e.getMessage)
          RED.println(s"Failed to start $serviceName!")
          throw e
      }
    }
  }

  def stop(): Unit = {
    if (enable) {
      GREEN.println(s"Stopping $serviceName ...")
      _stop(startResult)
      GREEN.println(s"Stopped $serviceName")
    }
  }
}
