package esw.ocs.framework

import akka.Done
import akka.actor.testkit.typed.Effect.{Spawned, Stopped, Watched}
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Props}
import esw.ocs.framework.GuardianActor.{ShutdownChildren, ShutdownReply, Spawn}

class GuardianActorTest extends BaseTestSuite {

  //create a dummy behaviour
  val echoBeh: Behaviors.Receive[String] = Behaviors.receiveMessage[String] { _ =>
    Behaviors.same[String]
  }

  "Spawn" should {
    "create a child actor of given behavior and return actor ref" in {

      val guardianActorTestKit = BehaviorTestKit(GuardianActor.behavior)
      val inbox                = TestInbox[ActorRef[String]]()

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

  "ShutdownChildren" when {
    "no children exist" should {
      "immediately return Done message" in {

        val guardianActorTestKit = BehaviorTestKit(GuardianActor.behavior)
        val inbox                = TestInbox[Done]()

        //send a shutdown children message to guardian
        guardianActorTestKit.run(ShutdownChildren(inbox.ref))

        inbox.expectMessage(Done)
      }
    }

    "children exist" should {
      "shutdown all children and send back Done message" in {
        val guardianActorTestKit = BehaviorTestKit(GuardianActor.behavior)
        val childActorRefInbox   = TestInbox[ActorRef[String]]()
        val shutDownMessageInbox = TestInbox[Done]()

        val emptyProps = Props.empty

        //create some child actors
        val child1 = "child-1"
        val child2 = "child-2"
        val child3 = "child-3"

        guardianActorTestKit.run(Spawn(echoBeh, child1, emptyProps)(childActorRefInbox.ref))
        guardianActorTestKit.run(Spawn(echoBeh, child2, emptyProps)(childActorRefInbox.ref))
        guardianActorTestKit.run(Spawn(echoBeh, child3, emptyProps)(childActorRefInbox.ref))

        //clearing all Effects (Spawned Effects)
        guardianActorTestKit.retrieveAllEffects()

        //ensure that we received 3 child actor refs
        childActorRefInbox.receiveAll() should have size 3

        //collect all 3 child actor refs
        val child1Ref = guardianActorTestKit.childInbox[String](child1).ref
        val child2Ref = guardianActorTestKit.childInbox[String](child2).ref
        val child3Ref = guardianActorTestKit.childInbox[String](child3).ref

        //send a shutdown children message to guardian
        guardianActorTestKit.run(ShutdownChildren(shutDownMessageInbox.ref))

        //ensure that all children are watched and stopped
        guardianActorTestKit.expectEffect(Watched[String](child1Ref))
        guardianActorTestKit.expectEffect(Stopped(child1))

        guardianActorTestKit.expectEffect(Watched[String](child2Ref))
        guardianActorTestKit.expectEffect(Stopped(child2))

        guardianActorTestKit.expectEffect(Watched[String](child3Ref))
        guardianActorTestKit.expectEffect(Stopped(child3))

        //In real environment, actor system will send this message but since this is unit test,
        //we need to send `ShutdownReply` manually
        guardianActorTestKit.run(ShutdownReply(shutDownMessageInbox.ref))
        //ensure that Done message is received
        shutDownMessageInbox.expectMessage(Done)
      }
    }
  }
}
