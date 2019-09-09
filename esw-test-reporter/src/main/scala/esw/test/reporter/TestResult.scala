package esw.test.reporter

case class TestResult(name: String, status: String) {
  //csv representation
  def toCSV: String = s"$name, $status"
}

case class StoryResult(name: String, tests: List[TestResult]) {
  //csv representation
  def toCSV: String = {
    val NEWLINE = "\n"
    tests.map(x => s"$name, ${x.toCSV}").reduce(_ + NEWLINE + _) + NEWLINE
  }
}
