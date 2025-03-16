package esw.testcommons

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}

trait ActorTestSuit extends BaseTestSuite {
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), getClass.getSimpleName)

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }
}
