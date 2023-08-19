package esw.ocs.dsl.jdk

import scala.concurrent.Future
import java.util.concurrent.CompletionStage

fun <T> Future<T>.toJava(): CompletionStage<T> = scala.jdk.javaapi.FutureConverters.asJava(this)
