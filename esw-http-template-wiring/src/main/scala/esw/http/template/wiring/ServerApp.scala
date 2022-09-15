/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.http.template.wiring

import caseapp.core.app.CommandApp
import csw.location.api.models.Metadata
import esw.constants.CommonTimeouts

import scala.concurrent.Await
import scala.util.control.NonFatal

trait ServerApp[T] extends CommandApp[T] {

  def start(wiring: ServerWiring, metadata: Metadata): Unit = {
    try {
      wiring.actorRuntime.startLogging(progName, appVersion)
      wiring.logger.debug(s"starting $appName")
      val (binding, _) = Await.result(wiring.start(metadata), CommonTimeouts.Wiring)
      wiring.logger.info(s"$appName online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
    }
    catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
        wiring.logger.error(s"$appName crashed")
        exit(1)
    }
  }
}
