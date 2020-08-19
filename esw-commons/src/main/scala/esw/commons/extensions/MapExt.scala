package esw.commons.extensions

object MapExt {

  implicit class MapOps[K, V](private val map: Map[K, V]) extends AnyVal {
    def addKeyIfNotExist(key: K, defaultValue: V): Map[K, V] =
      map.updatedWith(key)(_.fold(Some(defaultValue))(Some(_)))

    def addKeysIfNotExist(keys: List[K], defaultValue: V): Map[K, V] =
      keys.flatMap(addKeyIfNotExist(_, defaultValue)).toMap
  }

}
