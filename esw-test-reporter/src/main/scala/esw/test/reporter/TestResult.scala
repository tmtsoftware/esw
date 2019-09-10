package esw.test.reporter

case class TestResult(name: String, status: String) {
  //csv representation
  def format(fieldSeparator: Char): String = s"$name $fieldSeparator $status"
}

case class StoryResult(name: String, tests: List[TestResult]) {
  //csv representation
  def format(fieldSeparator: Char, dataSeparator: Char): String = {
    tests
      .map { x =>
        s"$name $fieldSeparator ${x.format(fieldSeparator)}"
      }
      .reduce(_ + dataSeparator + _) + dataSeparator
  }
}
