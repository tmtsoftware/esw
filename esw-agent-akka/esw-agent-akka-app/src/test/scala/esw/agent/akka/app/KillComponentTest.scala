package esw.agent.akka.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand.KillComponent
import esw.agent.service.api.models._
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.duration.DurationLong

class KillComponentTest extends AgentSetup {

  Table(
    ("Name", "SpawnCommand", "Connection"),
    ("Redis", spawnRedis, redisConn),
    ("SequenceComponent", spawnSequenceComp, seqCompConn),
    ("SequenceManager", spawnSequenceManager, seqManagerConn)
  ).foreach {
    case (name, spawnComponent, connection) =>
      val componentId = connection.componentId

      s"KillComponent [$name]" must {
        s"reply Killed after stopping a registered component gracefully | ESW-276" in {
          agentWithSpawnedComponent(name + "1") { (agentActorRef, killResponseProbe) =>
            agentActorRef ! KillComponent(killResponseProbe.ref, componentId)
            killResponseProbe.expectMessage(Killed)

            //ensure component was unregistered
            verify(locationService, timeout(500)).unregister(connection)
          }
        }

        s"reply 'Killed' when process is already stopping by another message [Idempotent] | ESW-276" in {
          agentWithSpawnedComponent(name + "2") { (agentActorRef, killResponseProbe) =>
            agentActorRef ! KillComponent(killResponseProbe.ref, componentId)
            agentActorRef ! KillComponent(killResponseProbe.ref, componentId)

            //ensure it is stopped gracefully
            killResponseProbe.expectMessage(Killed)
            killResponseProbe.expectMessage(Killed)

            //ensure component was unregistered
            verify(locationService, timeout(500)).unregister(connection)
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

  "KillComponent must reply 'Failed' when given component is not running on agent | ESW-276" in {
    val agentActorRef = spawnAgentActor(name = "test-actor7")
    val probe         = TestProbe[KillResponse]()

    //try to stop the component
    val id = ComponentId(Prefix("ESW.invalid"), Service)
    agentActorRef ! KillComponent(probe.ref, id)

    //verify that response is Failure
    probe.expectMessage(Failed(s"Component ${id.fullName} is not running on this agent"))

    //ensure component was NOT unregistered
    verify(locationService, never).unregister(redisConn)
  }
}
