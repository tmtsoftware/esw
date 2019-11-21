package esw.ocs.dsl.jdk

import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import java.util.concurrent.CompletionStage

fun <T> Future<T>.toJava(): CompletionStage<T> = FutureConverters.toJava(this)