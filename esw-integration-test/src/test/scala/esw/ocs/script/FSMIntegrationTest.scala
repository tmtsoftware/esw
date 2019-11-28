package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType.{LongKey, StringKey}
import csw.params.core.models.Prefix
import csw.params.events.EventKey
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit

class FSMIntegrationTest extends EswTestKit(EventServer) {

  "FSM" must {

    "start child fsm and accept commands" ignore {
      val mainFsmKey = EventKey("esw.commandFSM.state")
      val tempFsmKey = EventKey("esw.temperatureFSM.state")
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
      mainFsmStateProbe.expectMessage("TERMINATE")

      fsmSequencer.stop().awaitResult
      mainFsmStateProbe.expectMessage("FSM:TERMINATE:STOP")
      mainFsmStateProbe.expectMessage("MAIN:STOP")
    }
  }

}
