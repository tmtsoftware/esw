package esw.ocs.testData

import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.{BooleanKey, StringKey, UTCTimeKey}
import csw.params.core.models.Units.NoUnits
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.{TAITime, TMTTime, UTCTime}
import esw.ocs.dsl.{CswServices, Script}

import scala.concurrent.duration.DurationDouble

class TestScript(csw: CswServices) extends Script(csw) {

  handleSetupCommand("command-1") { command =>
    spawn {
      // To avoid sequencer to finish immediately so that other Add, Append command gets time
      Thread.sleep(200)
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("command-2") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("command-3") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("command-4") { command =>
    spawn {
      //try sending concrete sequence
      val tcsSequencer = locationService.resolveSequencer("TCS", "testObservingMode4").await
      val command4     = Setup(Id("testCommandIdString123"), Prefix("TCS.test"), CommandName("command-to-assert-on"), None, Set.empty)
      val sequence     = Sequence(Id("testSequenceIdString123"), Seq(command4))

      // ESW-145, ESW-195
      csw.sequencerCommandService.submitSequence(tcsSequencer, sequence).await
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("fail-command") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Error(command.runId, command.commandName.name))
    }
  }

  handleSetupCommand("event-command") { command =>
    spawn {
      val param = StringKey.make("filter-wheel").set("a", "b", "c").withUnits(NoUnits)
      val event = eventService.systemEvent("TCS.test", "event-1", param)

      // ***** commonly used dsl ****
      eventService.publish(event).await

      eventService.publish(5.seconds) {
        if (true) Some(event)
        else None
      }
      // *****************************

      eventService.publish(5.seconds, UTCTime.now()) {
        if (true) Some(event)
        else None
      }

      eventService.publish(5.seconds)({
        if (true) Some(event)
        else None
      }, onError = {
        println
      })

      eventService.publish(5.seconds, TAITime.now())({
        if (true) Some(event)
        else None
      }, onError = {
        println
      })

      // ***************************************************

      val subscription = eventService.subscribe("TCS.test.event-1") { event =>
        println(event)
      }
      subscription.ready().await

      // ***************************************************

      val eventsF = eventService.get("TCS.test.event-1")
      val events  = eventsF.await
      events.foreach(println)
    }
  }

  handleSetupCommand("time-command") { command =>
    spawn {

      /************************** Schedule task once at particular time ************************************/
      val startTime = UTCTime(UTCTime.now().value.plusSeconds(10))

      timeService.scheduleOnce(startTime) {
        println("task")
      }

      /****************** Schedule task periodically at provided interval **********************************/
      timeService.schedulePeriodically(5.millis) {
        println("task")
      }

      /*************** Schedule task periodically at provided interval with start time *********************/
      val timeKey             = UTCTimeKey.make("time")
      val startTime1: TMTTime = command(timeKey).values.head

      timeService.schedulePeriodically(5.millis, startTime1) {
        println("task")
      }

    }

  }

  handleGoOnline {
    spawn {
      // do some actions to go online
      val param = BooleanKey.make("online").set(true)
      val event = SystemEvent(Prefix("TCS.test"), EventName("online")).add(param)
      eventService.publish(event).await
    }
  }

  handleGoOffline {
    spawn {
      // do some actions to go offline
      val param = BooleanKey.make("offline").set(true)
      val event = SystemEvent(Prefix("TCS.test"), EventName("offline")).add(param)
      eventService.publish(event).await
    }
  }
}
