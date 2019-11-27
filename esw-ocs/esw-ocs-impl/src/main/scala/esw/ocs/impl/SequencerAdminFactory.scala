package esw.ocs.impl

import java.util.concurrent.CompletionStage

import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import esw.ocs.api.SequencerAdminApi
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future

//fixme: why is it taking LocationServiceUtil, should this factory reside in DSL
class SequencerAdminFactory(locationServiceUtil: LocationServiceUtil)(implicit timeout: Timeout) {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  private def make(packageId: String, observingMode: String): Future[SequencerAdminApi] =
    locationServiceUtil
      .resolveSequencer(packageId, observingMode)
      .map(akkaLocation => new SequencerAdminImpl(akkaLocation.sequencerRef))

  def jMake(packageId: String, observingMode: String): CompletionStage[SequencerAdminApi] =
    make(packageId, observingMode).toJava
}
