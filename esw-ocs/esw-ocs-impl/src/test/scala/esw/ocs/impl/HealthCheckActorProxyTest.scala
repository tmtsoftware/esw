package esw.ocs.impl

import java.time.Duration

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import esw.ocs.api.BaseTestSuite
import esw.ocs.impl.messages.HealthCheckMsg
import esw.ocs.impl.messages.HealthCheckMsg.{Heartbeat, HeartbeatMissed}

import scala.concurrent.ExecutionContext

class HealthCheckActorProxyTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  implicit val ec: ExecutionContext = system.executionContext
  var heartbeat                     = false

  private val mockedBehavior: Behavior[HealthCheckMsg] = Behaviors.receiveMessage[HealthCheckMsg] { msg =>
    msg match {
      case Heartbeat =>
        println("done")
        heartbeat = true
      case HeartbeatMissed => heartbeat = false
    }
    Behaviors.same
  }

  private val healthCheckActor  = spawn(mockedBehavior)
  private val heartbeatInterval = Duration.ofSeconds(1)

  private val healthCheckActorProxy = new HealthCheckActorProxy(healthCheckActor, heartbeatInterval)

  "SendHeartbeat | ESW-290" in {
    healthCheckActorProxy.sendHeartbeat()
    eventually(heartbeat shouldBe true)
  }
}
