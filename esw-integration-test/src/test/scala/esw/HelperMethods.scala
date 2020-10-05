package esw

object HelperMethods {
  def getPidOf(searchPhrase: String): Long = {
    ProcessHandle
      .allProcesses()
      .filter(_.info().toString.contains(searchPhrase))
      .findFirst()
      .get()
      .toString
      .toLong
  }
}
