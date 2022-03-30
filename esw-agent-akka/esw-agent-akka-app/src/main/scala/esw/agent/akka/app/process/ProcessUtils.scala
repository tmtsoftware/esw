/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.akka.app.process

/**
 * This object allows verification of native CLI applications whether they are installed or not. It prevents unnecessary `command not found` error.
 */
object ProcessUtils {
  def isInstalled(cmd: String): Boolean = new ProcessBuilder("bash", "-c", s"command -v $cmd").start().waitFor() == 0
}
