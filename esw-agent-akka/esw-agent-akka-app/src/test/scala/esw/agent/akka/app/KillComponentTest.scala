package esw.agent.akka.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand.KillComponent
import esw.agent.service.api.models._
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.duration.DurationLong

class KillComponentTest extends AgentSetup {

  Table(
    ("Name", "SpawnCommand", "Location"),
    ("SequenceComponent", spawnSequenceComp, seqCompLocation),
    ("SequenceManager", spawnSequenceManager, seqManagerLocation),
    ("Container", spawnContainer, firstContainerLocation)
  ).foreach {
    case (name, spawnComponent, location) =>
      s"KillComponent [$name]" must {
        s"reply Killed after stopping a registered component gracefully | ESW-276, ESW-367" in {
          agentWithSpawnedComponent(name + "1") { (agentActorRef, killResponseProbe) =>
            agentActorRef ! KillComponent(killResponseProbe.ref, location)
            killResponseProbe.expectMessage(Killed)

            //ensure component was unregistered
            verify(locationService, timeout(500)).unregister(location.connection)
          }
        }

        s"reply 'Killed' when process is already stopping by another message [Idempotent] | ESW-276, ESW-367" in {
          agentWithSpawnedComponent(name + "2") { (agentActorRef, killResponseProbe) =>
            agentActorRef ! KillComponent(killResponseProbe.ref, location)
            agentActorRef ! KillComponent(killResponseProbe.ref, location)

            //ensure it is stopped gracefully
            killResponseProbe.expectMessage(Killed)
            killResponseProbe.expectMessage(Killed)

            //ensure component was unregistered
            verify(locationService, timeout(500)).unregister(location.connection)
          }
        }

        def agentWithSpawnedComponent(name: String)(testCode: (ActorRef[AgentCommand], TestProbe[KillResponse]) => Unit): Unit = {
          val agentActorRef      = spawnAgentActor(name = name)
          val spawnResponseProbe = TestProbe[SpawnResponse]()
          val killResponseProbe  = TestProbe[KillResponse]()

          mockLocationService()
          mockSuccessfulProcess(dieAfter = 500.millis)

          agentActorRef ! spawnComponent(spawnResponseProbe.ref)
          //wait till it is registered
          spawnResponseProbe.expectMessage(Spawned)
          testCode(agentActorRef, killResponseProbe)
        }
      }
  }
}
