package esw.ocs.testData

import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.{BooleanKey, StringKey, UTCTimeKey}
import csw.params.core.models.Units.NoUnits
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.UTCTime
import esw.highlevel.dsl.Util.RichCommand
import esw.ocs.impl.dsl.{CswServices, Script}

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
      val tcsSequencer = csw.resolveSequencer("TCS", "testObservingMode4").await
      val command4     = Setup(Id("testCommandIdString123"), Prefix("TCS.test"), CommandName("command-to-assert-on"), None, Set.empty)
      val sequence     = Sequence(Id("testSequenceIdString123"), Seq(command4))

      // ESW-145, ESW-195
      csw.submitSequence(tcsSequencer, sequence).await
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
      val event = csw.systemEvent("TCS.test", "event-1", param)

      // ***************************************************
      csw.publishEvent(event).await

      csw.publishEvent(5.seconds) {
        if (true) Some(event)
        else None
      }

      // ***************************************************
      csw.onEvent("TCS.test.event-1") { event =>
        println(event)
      }

      val events = csw.getEvent("TCS.test.event-1").await
      events.foreach(println)
    }
  }

  handleSetupCommand("time-command") { command =>
    spawn {

      /************************** Schedule task once at particular time ************************************/
      val startTime = UTCTime.after(10.millis)

      csw.scheduleOnce(startTime) {
        println("task")
      }

      /****************** Schedule task periodically at provided interval **********************************/
      csw.schedulePeriodically(5.millis) {
        println("task")
      }

      /*************** Schedule task periodically at provided interval with start time *********************/
      val utcTime = command.getParam("time-key", UTCTimeKey)

      csw.schedulePeriodically(5.millis, utcTime) {
        println("task")
      }

    }

  }

  handleGoOnline {
    spawn {
      // do some actions to go online
      val param = BooleanKey.make("online").set(true)
      val event = SystemEvent(Prefix("TCS.test"), EventName("online")).add(param)
      csw.publishEvent(event).await
    }
  }

  handleGoOffline {
    spawn {
      // do some actions to go offline
      val param = BooleanKey.make("offline").set(true)
      val event = SystemEvent(Prefix("TCS.test"), EventName("offline")).add(param)
      csw.publishEvent(event).await
    }
  }

  handleDiagnosticMode {
    case (startTime, hint) =>
      spawn {
        // do some actions to go to diagnostic mode based on hint
        csw.diagnosticMode("test", Assembly, startTime, hint)
      }
  }

  handleOperationsMode {
    spawn {
      // do some actions to go to operations mode
    }
  }
}
