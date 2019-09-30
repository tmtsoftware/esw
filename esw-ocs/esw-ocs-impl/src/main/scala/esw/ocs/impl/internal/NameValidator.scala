package esw.ocs.impl.internal

object NameValidator {
  def validate(name: String): Unit = {
    val invalidSymbol = "@"
    require(!name.contains(invalidSymbol))
  }
}
