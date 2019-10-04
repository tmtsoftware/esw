package esw.ocs.impl

import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import esw.ocs.api.{SequencerAdminApi, SequencerAdminFactoryApi}
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.concurrent.Future

class SequencerAdminFactoryImpl(locationServiceUtil: LocationServiceUtil)(implicit timeout: Timeout)
    extends SequencerAdminFactoryApi {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  def make(packageId: String, observingMode: String): Future[SequencerAdminApi] =
    locationServiceUtil
      .resolveSequencer(packageId, observingMode)
      .map(akkaLocation => new SequencerAdminImpl(akkaLocation.sequencerRef))
}
