package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType.IntKey
import csw.params.events.EventKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import csw.testkit.scaladsl.CSWService.EventServer

class ThreadSafetyTest extends EswTestKit(EventServer) {

  "Script" must {

    // 1. Main script starts background job in a constructor which increments counter 100_000 times
    // 2. Reusable script starts background job in a constructor which increments counter 100_000 times
    // 3. Concurrently script receives, increment cmd which increments counter 100_000 times
    // 4. All the above 4 steps runs concurrently
    // 5. This test verifies that in the end counter is 300_000
    // this test will fail if you change StrandEc.apply() = new StrandEc(Executors.newScheduledThreadPool(4))
    "handle concurrent shared mutable state safely | ESW-133, ESW-185" in {
      val counter    = EventKey("esw.counter.get-counter")
      val counterKey = IntKey.make("counter")

      val counterProbe = TestProbe[Int]()

      eventSubscriber
        .subscribeCallback(
          Set(counter),
          event => {
            val param = event.paramType.get(counterKey).flatMap(_.get(0))
            param.foreach(counterProbe.ref ! _)
          }
        )

      val threadSafeSequencer: SequencerApi = spawnSequencerProxy(ESW, "threadSafe")

      val incrementCommand  = Setup(Prefix("esw.counter"), CommandName("increment"), None)
      val getCounterCommand = Observe(Prefix("esw.counter"), CommandName("get-counter"), None)

      threadSafeSequencer.submit(Sequence(incrementCommand, getCounterCommand))

      counterProbe.expectMessage(300000)
    }
  }
}
