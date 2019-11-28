package esw.ocs.impl

import java.util.concurrent.CompletionStage

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.SequencerInsight
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future

//fixme: why is it taking LocationServiceUtil, should this factory reside in DSL
class SequencerActorProxyFactory(locationServiceUtil: LocationServiceUtil)(implicit timeout: Timeout) {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  private def make(packageId: String, observingMode: String): Future[SequencerApi] =
    locationServiceUtil
      .resolveSequencer(packageId, observingMode)
      .map(akkaLocation => new SequencerActorProxy(akkaLocation.sequencerRef))

  def jMake(packageId: String, observingMode: String): CompletionStage[SequencerApi] =
    make(packageId, observingMode).toJava
}
