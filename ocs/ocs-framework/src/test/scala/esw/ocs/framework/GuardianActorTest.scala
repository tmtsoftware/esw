package esw.ocs.framework

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Props}
import esw.ocs.framework.GuardianActor.Spawn

class GuardianActorTest extends BaseTestSuite {
  "Spawn" should {
    "create a child actor of given behavior and return actor ref" in {

      val guardianActorTestKit = BehaviorTestKit(GuardianActor.behavior)
      val inbox                = TestInbox[ActorRef[String]]()

      //create a dummy behaviour
      val echoBeh: Behaviors.Receive[String] = Behaviors.receiveMessage[String] { _ =>
        Behaviors.same[String]
      }

      val actorName  = "child-actor"
      val emptyProps = Props.empty

      //send spawn message to guardian actor
      guardianActorTestKit.run(Spawn(echoBeh, actorName, emptyProps)(inbox.ref))

      //ensure that a child actor was created using `echoBeh`
      guardianActorTestKit.expectEffect(Spawned(echoBeh, actorName, emptyProps))

      //ensure that we get the child actor ref back in our inbox
      val childActor = inbox.receiveMessage()
      childActor shouldBe an[ActorRef[String]]

      //send a test message to child actor to ensure it's created from the same behavior
      val testMsg = "test-message"
      childActor ! testMsg

      //ensure that child actor received the test messaged
      guardianActorTestKit.childInbox[String](actorName).expectMessage(testMsg)
    }
  }
}
