package esw.ocs.dsl.jdk

import scala.jdk.FutureConverters.*
import scala.concurrent.Future
import java.util.concurrent.CompletionStage

fun <T> Future<T>.toJava(): CompletionStage<T> = FutureConverters.toJava(this)