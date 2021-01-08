package esw.services.internal

import csw.services.utils.ColoredConsole.{GREEN, RED}

import scala.util.control.NonFatal

case class ManagedService[T](
    serviceName: String,
    enable: Boolean,
    private val _start: () => T,
    private val _stop: T => Unit
) {
  private var startResult: Option[T] = None

  def start(): Unit = {
    if (enable) {
      try {
        GREEN.println(s"Starting $serviceName ...")
        startResult = Some(_start())
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
    GREEN.println(s"Stopping $serviceName ...")
    startResult.foreach(_stop)
    GREEN.println(s"Stopped $serviceName")
  }
}
