package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.generics.KeyType.{IntKey, LongKey, StringKey}
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class FsmIntegrationTest extends EswTestKit(EventServer) {

  "Fsm" must {

    "start child Fsm and accept commands | ESW-246, ESW-251, ESW-142" in {
      val mainFsmKey = EventKey("esw.commandFsm.state")
      val tempFsmKey = EventKey("esw.temperatureFsm.state")
      val stateKey   = StringKey.make("state")

      val mainFsmStateProbe = TestProbe[String]()
      val tempFsmStateProbe = TestProbe[String]()

      eventSubscriber
        .subscribeCallback(
          Set(mainFsmKey),
          event => {
            val param = event.paramType.get(stateKey).flatMap(_.get(0))
            param.foreach(mainFsmStateProbe.ref ! _)
          }
        )

      eventSubscriber
        .subscribeCallback(
          Set(tempFsmKey),
          event => {
            val param = event.paramType.get(stateKey).flatMap(_.get(0))
            param.foreach(tempFsmStateProbe.ref ! _)
          }
        )

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

      tempFsmStateProbe.expectMessage("ERROR")
      tempFsmStateProbe.expectMessage("OK")
      tempFsmStateProbe.expectMessage("FINISHED")
      mainFsmStateProbe.expectMessage("TERMINATE")

      fsmSequencer.stop().futureValue
      mainFsmStateProbe.expectMessage("Fsm:TERMINATE:STOP")
      mainFsmStateProbe.expectMessage("MAIN:STOP")
    }

    "pass parameters to next state via become | ESW-246, ESW-251, ESW-252, ESW-142" in {
      val temperatureFsmKey = IntKey.make("temperatureFsm")
      val commandKey        = KeyType.IntKey.make("command")
      val fsmStateProbe     = TestProbe[Int]()

      eventSubscriber
        .subscribeCallback(
          Set(EventKey("esw.FsmTestScript.WAITING")),
          event => {
            val param = event.paramType.get(temperatureFsmKey).flatMap(_.get(0))
            param.foreach(fsmStateProbe.ref ! _)
          }
        )

      eventSubscriber
        .subscribeCallback(
          Set(EventKey("esw.FsmTestScript.STARTED")),
          event => {
            val param = event.paramType.get(commandKey).flatMap(_.get(0))
            param.foreach(fsmStateProbe.ref ! _)
          }
        )

      val fsmSequencer: SequencerApi = spawnSequencerProxy(ESW, "becomeFsm")
      val command1                   = Setup(Prefix("esw.test"), CommandName("command-1"), None).madd(commandKey.set(10))
      val command2                   = Setup(Prefix("esw.test"), CommandName("command-2"), None)

      Await.result(fsmSequencer.submitAndWait(Sequence(command1, command2)), 10.seconds)

      eventually {
        fsmStateProbe.expectMessage(20)
        fsmStateProbe.expectMessage(10)
      }

      fsmSequencer.stop().futureValue
    }

    "command flag should trigger FSM bind to it | ESW-246, ESW-251, ESW-252, ESW-142" in {
      val observeFsmKey = IntKey.make("observe")
      val fsmStateProbe = TestProbe[Int]()

      eventSubscriber
        .subscribeCallback(
          Set(EventKey("esw.CommandFlagFsmTestScript.OBSERVE")),
          event => {
            val param = event.paramType.get(observeFsmKey).flatMap(_.get(0))
            param.foreach(fsmStateProbe.ref ! _)
          }
        )

      val fsmSequencer: SequencerApi = spawnSequencerProxy(ESW, "commandFlagFsm")
      val command1                   = Observe(Prefix("esw.test"), CommandName("observe-command-1"), None)
      val command2                   = Observe(Prefix("esw.test"), CommandName("observe-command-2"), None).madd(observeFsmKey.set(100))

      Await.result(fsmSequencer.submitAndWait(Sequence(command1, command2)), 10.seconds)

      eventually {
        fsmStateProbe.expectMessage(100)
      }

      fsmSequencer.stop().futureValue
    }

    "be able to bind to param variables with polling time | ESW-142, ESW-256, ESW-291" in {
      val subsystem                  = ESW
      val observingMode              = "MoonNight"
      val fsmSequencer: SequencerApi = spawnSequencerProxy(subsystem, observingMode)
      val command1                   = Setup(Prefix("esw.test"), CommandName("start-param-fsm"), None)
      val probe                      = TestProbe[Event]()

      eventSubscriber.subscribeActorRef(Set(EventKey("tcs.polling.param-var-test")), probe.ref)

      fsmSequencer.submit(Sequence(command1)).futureValue shouldBe a[Started]

      // this is to wait to publish 5 events, which asserts that INIT state is called 5 times. 1st time at 0 millies and
      // then next 4 at an interval of 400 millis
      Thread.sleep(1800)
      probe.receiveMessages(4)
    }

    "be able to bind to event variables with polling time | ESW-142, ESW-256, ESW-291" in {
      val subsystem     = ESW
      val observingMode = "EventVar"

      val fsmSequencer: SequencerApi = spawnSequencerProxy(subsystem, observingMode)
      val command1                   = Setup(Prefix("esw.test"), CommandName("start-event-fsm"), None)
      val probe                      = TestProbe[Event]()

      eventSubscriber.subscribeActorRef(Set(EventKey("tcs.polling.event-var-test")), probe.ref)

      fsmSequencer.submit(Sequence(command1)).futureValue shouldBe a[Started]

      // this is to wait to publish 5 events, which asserts that INIT state is called 5 times. 1st time at 0 millies and
      // then next 4 at an interval of 400 millis
      Thread.sleep(1800)
      probe.receiveMessages(4)
    }
  }
}
