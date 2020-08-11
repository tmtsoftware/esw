package esw.agent.akka.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import csw.location.api.models._
import csw.location.api.scaladsl.RegistrationResult
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand.GetComponentStatus
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.service.api.ComponentStatus.{Initializing, NotAvailable, Running}
import esw.agent.service.api.{ComponentStatus, SpawnResponse, Spawned}
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class GetComponentStatusTest extends AgentSetup {
  "GetComponentStatus" must {
    val agentPrefixStr                                    = "ESW.dummy-agent"
    val spawnRedis: ActorRef[SpawnResponse] => SpawnRedis = SpawnRedis(_, prefix, 6548, List.empty)
    val spawnSeqComponent: ActorRef[SpawnResponse] => SpawnSequenceComponent =
      SpawnSequenceComponent(_, agentPrefixStr, seqCompPrefix, None)

    s"reply 'NotAvailable' when given component is not present on machine | ESW-286" in {
      withAgentSetup("1") { (agentRef, compStatusProbe, _) =>
        agentRef ! GetComponentStatus(compStatusProbe.ref, componentId)
        compStatusProbe.expectMessage(NotAvailable)
      }
    }

    Table(
      ("name", "SpawnCommand", "ComponentID"),
      ("SpawnRedis", spawnRedis, componentId),
      ("SpawnSequenceComponent", spawnSeqComponent, seqCompComponentId),
      ("SpawnSequenceManager", spawnSequenceManager, seqManagerComponentId)
    ).foreach {
      case (name, spawnComponent, compId) =>
        s"reply 'Initializing' when component is not spawned [$name] | ESW-286" in {
          withAgentSetup(name + "2") { (agentRef, compStatusProbe, spawnResProbe) =>
            agentRef ! spawnComponent(spawnResProbe.ref)
            agentRef ! GetComponentStatus(compStatusProbe.ref, compId)
            compStatusProbe.expectMessage(Initializing)
          }
        }

        s"reply 'Running' when process is running and registered [$name] | ESW-286" in {
          withAgentSetup(name + "3", spawnComponentAfter = 0.seconds) { (agentRef, compStatusProbe, spawnResProbe) =>
            agentRef ! spawnComponent(spawnResProbe.ref)
            spawnResProbe.expectMessage(Spawned)
            agentRef ! GetComponentStatus(compStatusProbe.ref, compId)
            compStatusProbe.expectMessage(Running)
          }
        }
    }
  }

  private def withAgentSetup(name: String, spawnComponentAfter: FiniteDuration = 2.seconds)(
      testCode: (ActorRef[AgentCommand], TestProbe[ComponentStatus], TestProbe[SpawnResponse]) => Unit
  ): Unit = {
    val location           = mock[Location]
    val registrationResult = mock[RegistrationResult]
    val agentActorRef      = spawnAgentActor(name = name)
    val probe              = TestProbe[ComponentStatus]()
    val spawner            = TestProbe[SpawnResponse]()

    mockSuccessfulProcess(5.seconds) // do not let process die
    when(locationService.resolve(any[TypedConnection[Location]], any[FiniteDuration]))
      .thenReturn(delayedFuture(None, spawnComponentAfter), Future.successful(Some(location)))
    when(locationService.register(any[Registration])).thenReturn(Future.successful(registrationResult))

    testCode(agentActorRef, probe, spawner)
  }

}
