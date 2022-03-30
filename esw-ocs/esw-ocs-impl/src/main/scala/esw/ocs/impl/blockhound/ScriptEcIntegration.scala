/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.impl.blockhound

import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

/**
 * A class for BlockHoundIntegration contract for the thread on which sequencer scripts are to run
 *
 * @param scriptThreadName - thread name of the script
 */
class ScriptEcIntegration(scriptThreadName: String) extends BlockHoundIntegration {
  override def applyTo(builder: BlockHound.Builder): Unit = {
    builder
      .nonBlockingThreadPredicate(p => {
        p.or(it => it.getName.equals(scriptThreadName))
      })
      .allowBlockingCallsInside("java.io.PrintStream", "println")
      .allowBlockingCallsInside("csw.event.client.internal.redis.RedisPublisher", "publishInitializationEvent")
      .allowBlockingCallsInside("csw.params.core.models.Id$", "apply")
      .blockingMethodCallback(method => new Exception(method.toString).printStackTrace())
  }

  override def toString: String = s"[ScriptEcIntegration for $scriptThreadName]"
}
