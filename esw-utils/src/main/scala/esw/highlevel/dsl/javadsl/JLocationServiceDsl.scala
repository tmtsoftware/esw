package esw.highlevel.dsl.javadsl

import java.util
import java.util.Optional
import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import csw.location.models._
import csw.params.core.models.Subsystem
import esw.highlevel.dsl.LocationServiceDsl

import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.FutureOps

trait JLocationServiceDsl { self: LocationServiceDsl =>

  private[esw] def actorSystem: ActorSystem[_]

  // it is ok to pass actor system's ec, because only map operations on returned future requires it and does not mutate
  private[esw] implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  def jListBy(subsystem: Subsystem, componentType: ComponentType): CompletionStage[util.List[AkkaLocation]] =
    listBy(subsystem, componentType).map(_.asJava).asJava

  def jListByComponentName(name: String): CompletionStage[util.List[Location]] = {
    listByComponentName(name).map(_.asJava).asJava
  }

  def jResolveByComponentNameAndType(name: String, componentType: ComponentType): CompletionStage[Optional[Location]] =
    resolveByComponentNameAndType(name, componentType).map(_.asJava).asJava

  // To be used by Script Writer
  def jResolveSequencer(sequencerId: String, observingMode: String): CompletionStage[AkkaLocation] =
    resolveSequencer(sequencerId, observingMode).asJava

}
