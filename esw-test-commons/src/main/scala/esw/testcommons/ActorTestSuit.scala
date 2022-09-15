/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.testcommons

import akka.actor.typed.{ActorSystem, SpawnProtocol}

trait ActorTestSuit extends BaseTestSuite {
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), getClass.getSimpleName)

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }
}
