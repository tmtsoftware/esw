package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.generics.KeyType.{IntKey, LongKey, StringKey}
import csw.params.core.models.Prefix
import csw.params.events.EventKey
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import scala.concurrent.duration.DurationInt

import scala.concurrent.Await

class FsmIntegrationTest extends EswTestKit(EventServer) {

  "Fsm" must {

    "start child Fsm and accept commands | ESW-246, ESW-251" in {
      val mainFsmKey = EventKey("esw.commandFsm.state")
      val tempFsmKey = EventKey("esw.temperatureFsm.state")
      val stateKey   = StringKey.make("state")

      val mainFsmStateProbe = TestProbe[String]
      val tempFsmStateProbe = TestProbe[String]

      eventSubscriber
        .subscribeCallback(Set(mainFsmKey), event => {
          val param = event.paramType.get(stateKey).flatMap(_.get(0))
          param.foreach(mainFsmStateProbe.ref ! _)
        })

      eventSubscriber
        .subscribeCallback(Set(tempFsmKey), event => {
          val param = event.paramType.get(stateKey).flatMap(_.get(0))
          param.foreach(tempFsmStateProbe.ref ! _)
        })

      val fsmSequencer: SequencerApi = spawnSequencerProxy("esw", "fsm")

      mainFsmStateProbe.expectMessage("INIT")
      tempFsmStateProbe.expectMessage("OK")
      mainFsmStateProbe.expectMessage("STARTED")

      val tempKey     = LongKey.make("temperature")
      val baseTempCmd = Setup(Prefix("esw.test"), CommandName("set-temp"), None)

      val temp_45 = baseTempCmd.add(tempKey.set(45))
      val temp_30 = baseTempCmd.add(tempKey.set(30))
      val temp_55 = baseTempCmd.add(tempKey.set(55))

      val waitCmd = Observe(Prefix("esw.test"), CommandName("wait"), None)

      fsmSequencer.submit(Sequence(temp_45, temp_30, temp_55, waitCmd))

      println(fsmSequencer.getSequence.futureValue)

      tempFsmStateProbe.expectMessage("ERROR")
      tempFsmStateProbe.expectMessage("OK")
      tempFsmStateProbe.expectMessage("FINISHED")
      mainFsmStateProbe.expectMessage("TERMINATE")

      fsmSequencer.stop().awaitResult
      mainFsmStateProbe.expectMessage("Fsm:TERMINATE:STOP")
      mainFsmStateProbe.expectMessage("MAIN:STOP")
    }

    "pass parameters to next state via become | ESW-246, ESW-251" in {
      val temperatureFsmKey = IntKey.make("temperatureFsm")
      val commandKey        = KeyType.IntKey.make("command")
      val fsmStateProbe     = TestProbe[Int]

      eventSubscriber
        .subscribeCallback(
          Set(EventKey("esw.FsmTestScript.WAITING")),
          event => {
            val param = event.paramType.get(temperatureFsmKey).flatMap(_.get(0))
            param.foreach(fsmStateProbe.ref ! _)
          }
        )

      eventSubscriber
        .subscribeCallback(Set(EventKey("esw.FsmTestScript.STARTED")), event => {
          val param = event.paramType.get(commandKey).flatMap(_.get(0))
          param.foreach(fsmStateProbe.ref ! _)
        })

      val fsmSequencer: SequencerApi = spawnSequencerProxy("esw", "becomeFsm")
      val command1                   = Setup(Prefix("esw.test"), CommandName("command-1"), None).madd(commandKey.set(10))
      val command2                   = Setup(Prefix("esw.test"), CommandName("command-2"), None)

      Await.result(fsmSequencer.submitAndWait(Sequence(command1, command2)), 10.seconds)

      eventually {
        fsmStateProbe.expectMessage(20)
        fsmStateProbe.expectMessage(10)
      }

      fsmSequencer.stop().awaitResult
    }
  }
}
