package esw.ocs.api

import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future

trait SequencerAdminFactoryApi {
  def make(sequencerId: String, observingMode: String): Future[SequencerAdminApi]

  // Added this to be accessed by kotlin
  def jMake(sequencerId: String, observingMode: String): CompletionStage[SequencerAdminApi] =
    make(sequencerId, observingMode).toJava
}
