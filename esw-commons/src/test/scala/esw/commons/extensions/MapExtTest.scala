package esw.commons.extensions

import esw.commons.extensions.MapExt.MapOps
import esw.testcommons.BaseTestSuite

class MapExtTest extends BaseTestSuite {

  "addKeyIfNotExist" must {
    "return updated map with new entry added when provided key does not exist" in {
      Map("One" -> 1).addKeyIfNotExist("Two", 2) should ===(Map("One" -> 1, "Two" -> 2))
    }

    "return existing map unchanged when provided key exist" in {
      Map("One" -> 1).addKeyIfNotExist("One", 2) should ===(Map("One" -> 1))
    }
  }

  "addKeysIfNotExist" must {
    "return map which includes all new keys along with existing unchanged" in {
      val e1 = "first"  -> List(1, 2)
      val e2 = "second" -> List(3, 4)
      Map(e1, e2).addKeysIfNotExist(List("second", "third"), List.empty) should ===(Map(e1, e2, "third" -> List.empty))
    }
  }

}
