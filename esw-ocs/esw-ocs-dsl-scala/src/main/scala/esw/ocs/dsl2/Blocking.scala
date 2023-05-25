package esw.ocs.dsl2

import scala.concurrent.ExecutionContext

import async.Async._

class Blocking(defaultEc: ExecutionContext, blockingCpuEc: ExecutionContext, blockingIoEc: ExecutionContext) {
  inline def cpu[T](block: => T): T = withContext(blockingCpuEc)(block)
  inline def io[T](block: => T): T  = withContext(blockingIoEc)(block)

  private inline def withContext[T](contextEc: ExecutionContext)(block: => T): T =
    val resultF = async(block)(using contextEc)
    await(resultF)(using defaultEc)
}
