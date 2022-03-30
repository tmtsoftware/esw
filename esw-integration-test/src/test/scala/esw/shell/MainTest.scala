/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.shell

import java.util.concurrent.TimeUnit

import esw.testcommons.BaseTestSuite

class MainTest extends BaseTestSuite {

  "verify esw-shell compiles and starts successfully" in {
    val channel        = "https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json"
    val version        = "0.1.0-SNAPSHOT"
    val commands       = List("cs", "launch", "--channel", channel, s"esw-shell:$version")
    val processBuilder = new ProcessBuilder(commands: _*)
    val process        = processBuilder.start()
    Thread.sleep(10000)
    assert(process.isAlive, "esw-shell failed to start!")

    process.descendants().map(_.destroyForcibly())
    process.destroyForcibly().waitFor(30, TimeUnit.SECONDS)

    assert(!process.isAlive, "esw-shell process did not terminate within 10 seconds!")
  }

}
