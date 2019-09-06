package esw.highlevel.dsl.javadsl

import java.util
import java.util.Optional
import java.util.concurrent.CompletionStage

import csw.location.models._
import csw.params.core.models.Subsystem
import esw.highlevel.dsl.LocationServiceDsl

import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.FutureOps

trait JLocationServiceDsl { self: LocationServiceDsl =>

  def jListBy(subsystem: Subsystem, componentType: ComponentType)(
      implicit ec: ExecutionContext
  ): CompletionStage[util.List[AkkaLocation]] =
    listBy(subsystem, componentType).map(_.asJava).asJava

  def jListByComponentName(name: String)(implicit ec: ExecutionContext): CompletionStage[util.List[Location]] = {
    listByComponentName(name).map(_.asJava).asJava
  }

  def jResolveByComponentNameAndType(name: String, componentType: ComponentType)(
      implicit ec: ExecutionContext
  ): CompletionStage[Optional[Location]] =
    resolveByComponentNameAndType(name, componentType).map(_.asJava).asJava

  // To be used by Script Writer
  def jResolveSequencer(sequencerId: String, observingMode: String)(
      implicit ec: ExecutionContext
  ): CompletionStage[AkkaLocation] =
    resolveSequencer(sequencerId, observingMode).asJava

}
