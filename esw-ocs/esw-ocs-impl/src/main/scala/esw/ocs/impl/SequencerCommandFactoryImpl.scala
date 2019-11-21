package esw.ocs.impl

import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import esw.ocs.api.{SequencerCommandApi, SequencerCommandFactoryApi}
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.concurrent.Future

class SequencerCommandFactoryImpl(locationServiceUtil: LocationServiceUtil)(implicit timeout: Timeout)
    extends SequencerCommandFactoryApi {

  import locationServiceUtil.actorSystem
  import actorSystem.executionContext

  def make(packageId: String, observingMode: String): Future[SequencerCommandApi] =
    locationServiceUtil
      .resolveSequencer(packageId, observingMode)
      .map(akkaLocation => new SequencerCommandImpl(akkaLocation.sequencerRef))
}
