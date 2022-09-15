/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.services.internal

import csw.services.utils.ColoredConsole.{GREEN, RED}

import scala.util.control.NonFatal

/*
 * Model class created to start/stop a service(AgentService, Gateway, SM etc)
 */
case class ManagedService[T](
    serviceName: String,
    enable: Boolean,
    // hook to execute while starting the service
    private val _start: () => T,
    // hook to execute while stopping the service
    private val _stop: T => Unit
) {
  private var startResult: Option[T] = None

  // execute given start hook for the service
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

  // execute given stop hook for the service
  def stop(): Unit = {
    startResult.foreach(w => {
      GREEN.println(s"Stopping $serviceName ...")
      _stop(w)
      GREEN.println(s"Stopped $serviceName")
    })
  }
}
