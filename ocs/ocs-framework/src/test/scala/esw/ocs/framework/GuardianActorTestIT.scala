package esw.ocs.framework

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Props}
import esw.ocs.framework.GuardianActor.{ShutdownChildren, Spawn}

class GuardianActorTestIT extends ScalaTestWithActorTestKit with BaseTestSuite {
  //create a dummy behaviour
  val echoBeh: Behaviors.Receive[String] = Behaviors.receiveMessage[String] { _ =>
    Behaviors.same[String]
  }

  "GuardianActor" should {
    "spawn children and shutdown all children and send back Done message" in {
      val guardianActor        = spawn(GuardianActor.behavior)
      val childActorRefProbe   = createTestProbe[ActorRef[String]]()
      val shutDownMessageProbe = createTestProbe[Done]()

      val emptyProps = Props.empty

      //create some child actors
      val child1 = "child-1"
      val child2 = "child-2"
      val child3 = "child-3"

      guardianActor ! Spawn(echoBeh, child1, emptyProps)(childActorRefProbe.ref)
      childActorRefProbe.expectMessageType[ActorRef[String]]

      guardianActor ! Spawn(echoBeh, child2, emptyProps)(childActorRefProbe.ref)
      childActorRefProbe.expectMessageType[ActorRef[String]]

      guardianActor ! Spawn(echoBeh, child3, emptyProps)(childActorRefProbe.ref)
      childActorRefProbe.expectMessageType[ActorRef[String]]

      //send a shutdown children message to guardian
      guardianActor ! ShutdownChildren(shutDownMessageProbe.ref)

      //ensure that all children are watched and stopped
      shutDownMessageProbe.expectMessage(Done)
    }
  }
}
