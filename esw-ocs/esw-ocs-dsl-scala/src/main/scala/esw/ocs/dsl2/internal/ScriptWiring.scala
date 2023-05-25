package esw.ocs.dsl2.internal

import akka.actor.typed.ActorSystem
import csw.params.events.SequencerObserveEvent
import esw.ocs.dsl2.lowlevel.CswServices
import esw.ocs.dsl.script.{ScriptDsl, StrandEc}
import esw.ocs.dsl2.highlevel.{
  AlarmServiceDsl,
  CommandServiceDsl,
  ConfigServiceDsl,
  CswHighLevelDsl,
  DatabaseServiceDsl,
  EventServiceDsl,
  LocationServiceDsl,
  LoopDsl
}
import esw.ocs.impl.script.ScriptContext

import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.ExecutionContext
import scala.jdk.DurationConverters.JavaDurationOps

class ScriptWiring(scriptContext: ScriptContext) {
  val heartbeatInterval: Duration          = scriptContext.heartbeatInterval
  val strandEc: StrandEc                   = StrandEc()
  given ExecutionContext                   = strandEc.ec
  val dispatcher: ScheduledExecutorService = strandEc.executorService
  val cswServices: CswServices             = CswServices(scriptContext, strandEc)
  val heartbeatChannel                     = ???
  given ActorSystem[_]                     = scriptContext.actorSystem

  val scriptDsl: ScriptDsl = new ScriptDsl(
    scriptContext.sequenceOperatorFactory,
    scriptContext.jLogger,
    strandEc,
    () => shutdown()
  )

  val loopDsl = LoopDsl(strandEc)

  val cswHighLevelDsl: CswHighLevelDsl = CswHighLevelDsl(
    scriptContext.jLogger.asScala,
    cswServices,
    LocationServiceDsl(cswServices.locationService),
    ConfigServiceDsl(cswServices.configService),
    EventServiceDsl(cswServices.eventPublisher, cswServices.eventSubscriber),
    CommandServiceDsl(),
    AlarmServiceDsl(
      cswServices.alarmService,
      scriptContext.config.getDuration("csw-alarm.refresh-interval").toScala,
      loopDsl
    ),
    cswServices.timeService,
    DatabaseServiceDsl(cswServices.databaseServiceFactory, cswServices.locationService),
    loopDsl,
    scriptContext,
    strandEc,
    SequencerObserveEvent(scriptContext.prefix)
  )

  def shutdown(): Unit = {
    strandEc.shutdown()
  }
}
