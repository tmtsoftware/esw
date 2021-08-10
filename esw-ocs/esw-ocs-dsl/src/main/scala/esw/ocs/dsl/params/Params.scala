package esw.ocs.dsl.params

import csw.params.core.generics.{Parameter, ParameterSetType}

import java.util
import scala.jdk.CollectionConverters.*

/**
 * A wrapper class for set(in java) of Parameter.
 *
 * @param params - set(in java) of Parameters
 */
case class Params(params: util.Set[Parameter[_]] = util.Set.of()) extends ParameterSetType[Params] {

  /**
   * This method transform the parameter's set from java to scala models.
   *
   * @return set of [[csw.params.core.generics.Parameter]]
   */
  override def paramSet: Set[Parameter[_]] = params.asScala.toSet

  /**
   * This method is a factory method. which takes a set(scala) of Parameters and returns the [[Params]]
   *
   * @param data - set of Parameters
   * @return [[Params]]
   */
  override protected def create(data: Set[Parameter[_]]): Params = copy(params = data.asJava)
}
