package esw.ocs.impl

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import esw.ocs.api.models.SequencerInsight
import esw.ocs.api.{SequencerAdminApi, SequencerAdminFactoryApi}
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.concurrent.Future

class SequencerAdminFactoryImpl(locationServiceUtil: LocationServiceUtil, insightSource: Source[SequencerInsight, NotUsed])(
    implicit timeout: Timeout
) extends SequencerAdminFactoryApi {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  def make(packageId: String, observingMode: String): Future[SequencerAdminApi] =
    locationServiceUtil
      .resolveSequencer(packageId, observingMode)
      .map(akkaLocation => new SequencerAdminImpl(akkaLocation.sequencerRef, insightSource))
}
