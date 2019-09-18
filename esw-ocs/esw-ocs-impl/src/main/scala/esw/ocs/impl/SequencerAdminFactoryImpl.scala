package esw.ocs.impl

import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.{SequencerAdminApi, SequencerAdminFactoryApi}

import scala.concurrent.Future

class SequencerAdminFactoryImpl(locationServiceUtil: LocationServiceUtil)(implicit timeout: Timeout)
    extends SequencerAdminFactoryApi {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  def make(sequencerId: String, observingMode: String): Future[SequencerAdminApi] =
    locationServiceUtil
      .resolveSequencer(sequencerId, observingMode)
      .map(akkaLocation => new SequencerAdminImpl(akkaLocation.sequencerRef))
}
