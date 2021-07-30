package esw.ocs.api.utils

import scala.util.Random

object RandomUtils {
  def randomString(size: Int): String = Random.alphanumeric.take(size).mkString
  def randomString5(): String         = randomString(5)
}
