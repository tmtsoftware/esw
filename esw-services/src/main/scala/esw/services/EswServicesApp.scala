package esw.services

import akka.actor.CoordinatedShutdown
import caseapp.RemainingArgs
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import esw.commons.cli.EswCommandApp
import esw.services.cli.Command
import esw.services.cli.Command._

import scala.util.control.NonFatal

object EswServicesApp extends EswCommandApp[Command] {
  override def appName: String    = getClass.getSimpleName.dropRight(1)
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name
  println(s"starting $progName-$appVersion")

  val hostname: String = Networks().hostname

  override def run(command: Command, args: RemainingArgs): Unit =
    command match {
      case s: Start => run(s)
    }

  def run(command: Start): Unit = {
    val wiring = new Wiring(command)
    try {
      LoggingSystemFactory.start(appName, appVersion, hostname, wiring.actorSystem)
      wiring.start()
      CoordinatedShutdown(wiring.actorSystem).addJvmShutdownHook(wiring.stop())
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
        wiring.stop()
        exit(1)
    }
  }
}
