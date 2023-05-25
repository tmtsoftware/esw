package esw.ocs.dsl2

import scala.async.Async.*
import scala.concurrent.{ExecutionContext, Future}

class Utils(using ExecutionContext) {
  inline def par[T](inline tasks: => T*): List[T] =
    val resultsF = tasks.toList.map(t => async(t))
    await(Future.sequence(resultsF))
}
