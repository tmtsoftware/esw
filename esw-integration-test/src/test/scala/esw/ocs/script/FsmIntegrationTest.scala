package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.generics.KeyType.{IntKey, LongKey, StringKey}
import csw.params.core.models.Prefix
import csw.params.core.models.Subsystem.ESW
import csw.params.events.{Event, EventKey}
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

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

      val fsmSequencer: SequencerApi = spawnSequencerProxy(ESW, "fsm")

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

      val fsmSequencer: SequencerApi = spawnSequencerProxy(ESW, "becomeFsm")
      val command1                   = Setup(Prefix("esw.test"), CommandName("command-1"), None).madd(commandKey.set(10))
      val command2                   = Setup(Prefix("esw.test"), CommandName("command-2"), None)

      Await.result(fsmSequencer.submitAndWait(Sequence(command1, command2)), 10.seconds)

      eventually {
        fsmStateProbe.expectMessage(20)
        fsmStateProbe.expectMessage(10)
      }

      fsmSequencer.stop().awaitResult
    }

    "be able to bind to event variables with polling time | ESW-142, ESW-256" in {

      val fsmSequencer: SequencerApi = spawnSequencerProxy(ESW, "moonnight")
      val command1                   = Setup(Prefix("esw.test"), CommandName("start-fsm"), None)
      val probe                      = TestProbe[Event]

      eventSubscriber.subscribeActorRef(Set(EventKey("tcs.polling.test")), probe.ref)

      fsmSequencer.submit(Sequence(command1)).futureValue shouldBe a[Started]

      // this is to wait to publish 5 event, which asserts that INIT state is called 5 times. 1st time at 0 millies and
      // and then next 4 at interval of 400 millis
      Thread.sleep(1800)
      probe.receiveMessages(4)
    }
  }
}
