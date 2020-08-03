package esw.agent.app.ext

import java.nio.file.Paths

import akka.actor.typed.ActorRef
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.api.SpawnResponse
import esw.agent.app.ext.SpawnCommandExt.SpawnCommandOps
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.Tables.Table

class SpawnCommandExtTest extends BaseTestSuite {
  private val replyTo         = mock[ActorRef[SpawnResponse]]
  private val subsystem       = IRIS
  private val compName        = "dummy"
  private val prefix          = Prefix(subsystem, compName)
  private val channel         = "https://github.com/apps.json"
  private val version         = "1.0.0"
  private val obsModeConf     = "obsMode.conf"
  private val obsModeConfPath = Paths.get(obsModeConf)
  private val port            = 8080
  private val redisArgs       = List("-conf", "redis.conf")

  private val spawnSeqComp               = SpawnSequenceComponent(replyTo, prefix, None)
  private val spawnSeqCompWithVersion    = SpawnSequenceComponent(replyTo, prefix, Some(version))
  private val spawnSeqCompCmd            = s"cs launch --channel $channel ocs-app -- seqcomp -s $subsystem -n $compName"
  private val spawnSeqCompWithVersionCmd = s"cs launch --channel $channel ocs-app:$version -- seqcomp -s $subsystem -n $compName"

  private val spawnSeqMgr               = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None)
  private val spawnSeqMgrWithVersion    = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, Some(version))
  private val spawnSeqMgrCmd            = s"cs launch --channel $channel sequence-manager -- start -o $obsModeConf -l"
  private val spawnSeqMgrWithVersionCmd = s"cs launch --channel $channel sequence-manager:$version -- start -o $obsModeConf -l"

  private val spawnRedis                    = SpawnRedis(replyTo, prefix, port, List.empty)
  private val spawnRedisWithExtraArgs       = SpawnRedis(replyTo, prefix, port, redisArgs)
  private val spawnRedisCmd                 = s"redis-server --port $port"
  private val spawnRedisCmdWithExtraArgsCmd = s"redis-server ${redisArgs.mkString(" ")} --port $port "

  "SpawnCommand.executableCommandStr" must {
    Table(
      ("TestName", "SpawnCommand", "ExpectedCommandStr"),
      ("SpawnSequenceComponent", spawnSeqComp, spawnSeqCompCmd),
      ("SpawnSequenceComponent(version)", spawnSeqCompWithVersion, spawnSeqCompWithVersionCmd),
      ("SpawnSequenceManager", spawnSeqMgr, spawnSeqMgrCmd),
      ("SpawnSequenceManager(version)", spawnSeqMgrWithVersion, spawnSeqMgrWithVersionCmd),
      ("SpawnRedis", spawnRedis, spawnRedisCmd),
      ("SpawnRedis(args)", spawnRedisWithExtraArgs, spawnRedisCmdWithExtraArgsCmd)
    ).foreach {
      case (name, spawnCommand, expectedCommandStr) =>
        name in { spawnCommand.executableCommandStr(channel) should ===(expectedCommandStr.split(" ").toList) }
    }
  }
}
