package esw.performance.utils

object Timing {

  def measureTimeMillis[R](block: => R): (R, Long) = {
    val t0          = System.currentTimeMillis()
    val result      = block
    val t1          = System.currentTimeMillis()
    val elapsedTime = t1 - t0
    (result, elapsedTime)
  }

}
