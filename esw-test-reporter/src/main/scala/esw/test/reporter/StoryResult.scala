package esw.test.reporter

case class Requirement(story: String, number: String)

object Requirement {
  val EMPTY = "None"
}

case class StoryResult(story: String, test: String, status: String) {
  def format(separator: Char): String = s"$story $separator $test $separator $status"
}

case class TestRequirementMapped(story: String, reqNum: String, test: String, status: String) {
  def format(separator: Char): String = s"$story $separator $reqNum $separator $test $separator $status"
}

object Separators {
  val PIPE        = '|' // separator for test name and story number
  val NEWLINE     = '\n'
  val COMMA       = ',' // separator for multiple story number
  val PARENTHESIS = '('
}
