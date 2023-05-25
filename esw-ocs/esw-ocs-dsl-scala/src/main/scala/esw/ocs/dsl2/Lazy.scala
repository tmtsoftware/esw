package esw.ocs.dsl2

import java.util.concurrent.atomic.AtomicBoolean

class Lazy[T](input: => T):
  private val atomicBoolean = new AtomicBoolean(false)

  lazy val value: T =
    atomicBoolean.set(true)
    input

  def isInitialized: Boolean = atomicBoolean.get()
end Lazy
