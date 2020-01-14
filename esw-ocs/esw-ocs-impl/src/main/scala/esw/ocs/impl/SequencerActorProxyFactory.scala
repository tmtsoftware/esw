package esw.ocs.impl

import java.util.concurrent.CompletionStage

import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.prefix.models.Subsystem
import esw.ocs.api.SequencerApi
import esw.ocs.impl.internal.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future

class SequencerActorProxyFactory(locationServiceUtil: LocationServiceUtil)(implicit timeout: Timeout) {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  private def make(subsystem: Subsystem, observingMode: String): Future[SequencerApi] =
    locationServiceUtil
      .resolveSequencer(subsystem, observingMode)
      .map(akkaLocation => new SequencerActorProxy(akkaLocation.sequencerRef))

  def jMake(subsystem: Subsystem, observingMode: String): CompletionStage[SequencerApi] =
    make(subsystem, observingMode).toJava
}
