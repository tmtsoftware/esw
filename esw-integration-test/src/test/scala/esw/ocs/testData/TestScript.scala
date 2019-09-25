//package esw.ocs.testData
//
//import java.util.Collections.singleton
//import java.util.concurrent.CompletableFuture
//
//import csw.location.models.ComponentType.Assembly
//import csw.params.commands.CommandResponse.{Completed, Error}
//import csw.params.commands.{CommandName, Sequence, Setup}
//import csw.params.core.generics.KeyType.StringKey
//import csw.params.core.models.Units.NoUnits
//import csw.params.core.models.{Id, Prefix}
//import esw.dsl.script.{CswServices, Script}
//
//import scala.jdk.FutureConverters.CompletionStageOps
//
//class TestScript(csw: CswServices) extends Script(csw) {
//
//  handleSetupCommand("command-1") { command =>
//    spawn {
//      // To avoid sequencer to finish immediately so that other Add, Append command gets time
//      Thread.sleep(200)
//      csw.crm.addOrUpdateCommand(Completed(command.runId))
//    }
//  }
//
//  handleSetupCommand("command-2") { command =>
//    spawn {
//      csw.crm.addOrUpdateCommand(Completed(command.runId))
//    }
//  }
//
//  handleSetupCommand("command-3") { command =>
//    spawn {
//      csw.crm.addOrUpdateCommand(Completed(command.runId))
//    }
//  }
//
//  handleSetupCommand("command-4") { command =>
//    spawn {
//      //try sending concrete sequence
//      val command4 = Setup(Id("testCommandIdString123"), Prefix("TCS.test"), CommandName("command-to-assert-on"), None, Set.empty)
//      val sequence = Sequence(Id("testSequenceIdString123"), Seq(command4))
//
//      // ESW-145, ESW-195
//      csw.submitSequence("TCS", "testObservingMode4", sequence).asScala.await
//      csw.crm.addOrUpdateCommand(Completed(command.runId))
//    }
//  }
//
//  handleSetupCommand("fail-command") { command =>
//    spawn {
//      csw.crm.addOrUpdateCommand(Error(command.runId, command.commandName.name))
//    }
//  }
//
//  handleSetupCommand("event-command") { command =>
//    spawn {
//      val param = StringKey.make("filter-wheel").set("a", "b", "c").withUnits(NoUnits)
//      val event = csw.systemEvent("TCS.test", "event-1", singleton(param))
//
//      // ***************************************************
//      csw.publishEvent(event).asScala.await
//
////      csw.publishEvent(5.seconds, () => {
//      //        if (true) Some(event)
//      //        else None
//      //      })
//
//      // ***************************************************
//      csw.onEvent(singleton("TCS.test.event-1"), CompletableFuture.completedFuture)
//
//      csw.getEvent(singleton("TCS.test.event-1")).asScala.await
//    }
//  }
//
//  handleSetupCommand("time-command") { command =>
//    spawn {
//
//      /************************** Schedule task once at particular time ************************************/
////      val startTime = UTCTime.after(10.millis)
//
////      csw.scheduleOnce(startTime, Callback(() => println("task")))
//
//      /****************** Schedule task periodically at provided interval **********************************/
////      csw.schedulePeriodically(5.millis) {
//      //        println("task")
//      //      }
//
//      /*************** Schedule task periodically at provided interval with start time *********************/
////      val utcTime = command.getParam("time-key", UTCTimeKey)
//
////      csw.schedulePeriodically(5.millis, utcTime) {
//      //        println("task")
//      //      }
//
//    }
//
//  }
//
//  handleDiagnosticMode {
//    case (startTime, hint) =>
//      spawn {
//        // do some actions to go to diagnostic mode based on hint
//        csw.diagnosticModeForComponent("test", Assembly, startTime, hint)
//      }
//  }
//
//  handleOperationsMode {
//    spawn {
//      // do some actions to go to operations mode
//      csw.operationsModeForComponent("test", Assembly)
//    }
//  }
//}
