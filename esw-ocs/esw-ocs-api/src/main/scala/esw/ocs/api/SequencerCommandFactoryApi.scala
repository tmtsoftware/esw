package esw.ocs.api

import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future

trait SequencerCommandFactoryApi {
  def make(packageId: String, observingMode: String): Future[SequencerCommandApi]

  // Added this to be accessed by kotlin
  def jMake(packageId: String, observingMode: String): CompletionStage[SequencerCommandApi] =
    make(packageId, observingMode).toJava
}
