package esw.ocs.dsl.params

import java.util

import csw.params.core.generics.{Parameter, ParameterSetType}

import scala.jdk.CollectionConverters._

case class Params(params: util.Set[Parameter[_]] = util.Set.of()) extends ParameterSetType[Params] {
  override def paramSet: Set[Parameter[_]] = params.asScala.toSet

  override protected def create(data: Set[Parameter[_]]): Params = copy(params = data.asJava)
}
