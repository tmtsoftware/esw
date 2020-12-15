package esw.agent.akka.app

import java.nio.file.Path

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.prefix.models.Prefix
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand.{
  SpawnAAS,
  SpawnPostgres,
  SpawnRedis,
  SpawnSequenceComponent,
  SpawnSequenceManager
}
import esw.agent.service.api.models.{Failed, SpawnResponse, Spawned}
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SpawnComponentTest extends AgentSetup {

  "Spawn" must {
    "reply 'Spawned' and spawn sequence component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "ocs-app",
          "--",
          "seqcomp",
          "-s",
          agentPrefix.subsystem.name,
          "-n",
          seqCompName,
          "-a",
          agentPrefix.toString()
        )
      verify(processExecutor).runCommand(expectedCommand, seqCompPrefix)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()
      val err           = "Failed to resolve component"
      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.failed(new RuntimeException(err)))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"Failed to verify component registration in location service, reason: $err"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()
      when(locationService.find(argEq(seqCompConn))).thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"$seqCompComponentId is already registered with location service at $seqCompLocation"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"$seqCompComponentId is not registered with location service"))
    }

    "reply 'Failed' if process fails before registration is successful | ESW-367" in {
      val agentActorRef = spawnAgentActor(name = "test-actor10")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      mockSuccessfulProcess()
      when(process.isAlive).thenReturn(false)
      when(locationService.unregister(seqCompConn)).thenReturn(Future.successful(Done))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed("Process terminated before registration was successful"))
    }

    "reply 'Spawned' and spawn sequence manager process | ESW-180" in {
      val agentActorRef = spawnAgentActor(name = "test-actor9")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqManagerConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration])).thenReturn(seqManagerLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceManager(probe.ref, Path.of("obsMode.conf"), isConfigLocal = true, None)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "sequence-manager",
          "--",
          "start",
          "-o",
          "obsMode.conf",
          "-l",
          "-a",
          agentPrefix.toString()
        )

      verify(processExecutor).runCommand(expectedCommand, seqManagerPrefix)
    }

    "reply 'Spawned' and spawn redis using location agent | ESW-368" in {
      val agentActorRef = spawnAgentActor(name = "test-actor-redis")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(redisConnection))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(redisConnection), any[FiniteDuration])).thenReturn(redisServiceLocationF)

      mockSuccessfulProcess()

      val configPath = Path.of("redis-sentinel.conf")
      val port       = Some(8090)
      val version    = Some("0.1.0-SNAPSHOT")

      agentActorRef ! SpawnRedis(probe.ref, redisServicePrefix, configPath, port, version)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "location-agent:0.1.0-SNAPSHOT",
          "--",
          "--prefix",
          redisServicePrefix.toString(),
          "--command",
          s"redis-sentinel $configPath --port ${port.get}",
          "--port",
          port.get.toString,
          "-a",
          agentPrefix.toString()
        )

      verify(processExecutor).runCommand(expectedCommand, redisServicePrefix)
    }

    "reply 'Spawned' and spawn postgres using location agent | ESW-368" in {
      val agentActorRef = spawnAgentActor(name = "test-actor-postgres")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(postgresConnection))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(postgresConnection), any[FiniteDuration])).thenReturn(postgresServiceLocationF)

      mockSuccessfulProcess()

      val configPath       = Path.of("pg_hba.conf")
      val port             = Some(8090)
      val version          = Some("0.1.0-SNAPSHOT")
      val dbUnixSocketDirs = "/tmp"

      agentActorRef ! SpawnPostgres(probe.ref, postgresServicePrefix, configPath, port, dbUnixSocketDirs, version)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "location-agent:0.1.0-SNAPSHOT",
          "--",
          "--prefix",
          postgresServicePrefix.toString(),
          "--command",
          s"postgres --hba_file=$configPath --unix_socket_directories=$dbUnixSocketDirs -i -p ${port.get}",
          "--port",
          port.get.toString,
          "-a",
          agentPrefix.toString()
        )

      verify(processExecutor).runCommand(expectedCommand, postgresServicePrefix)
    }

    "reply 'Spawned' and spawn AAS using location agent | ESW-368" in {
      val agentActorRef = spawnAgentActor(name = "test-actor-aas")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(aasConnection))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(aasConnection), any[FiniteDuration])).thenReturn(aasServiceLocationF)

      mockSuccessfulProcess()

      val migrationFilePath = Path.of("tmt-realm-migration.json")
      val port              = Some(8090)
      val version           = Some("0.1.0-SNAPSHOT")
      val keycloakDir       = Path.of("keycloak-11.0.3")

      agentActorRef ! SpawnAAS(probe.ref, aasServicePrefix, keycloakDir, migrationFilePath, port, version)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "location-agent:0.1.0-SNAPSHOT",
          "--",
          "--prefix",
          aasServicePrefix.toString(),
          "--http",
          "auth",
          "--command",
          s"$keycloakDir/bin/standalone.sh -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=$migrationFilePath -Djboss.http.port=${port.get}",
          "--port",
          port.get.toString,
          "-a",
          agentPrefix.toString()
        )

      verify(processExecutor).runCommand(expectedCommand, aasServicePrefix)
    }

  }
}
