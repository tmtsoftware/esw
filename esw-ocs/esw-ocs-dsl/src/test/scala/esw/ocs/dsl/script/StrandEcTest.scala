/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script

import esw.ocs.impl.blockhound.{BlockHoundWiring, ScriptEcIntegration}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StrandEcTest extends AnyWordSpec with Matchers {
  "shutdown" must {
    "stop executor service" in {
      val strandEc = StrandEc()
      strandEc.shutdown()
      strandEc.executorService.isShutdown shouldBe true
    }
  }
  "apply" must {
    "create ScriptEcIntegration for script thread | ESW-290" in {
      // assert ScriptEcIntegration not added to BlockHound
      val previousIntegrations = BlockHoundWiring.integrations.count(_.isInstanceOf[ScriptEcIntegration])
      val strandEc             = StrandEc()
      // assert that creation of strandEc adds ScriptEcIntegration
      BlockHoundWiring.integrations.count(_.isInstanceOf[ScriptEcIntegration]) shouldBe (previousIntegrations + 1)
      strandEc.shutdown()
    }
  }
}
