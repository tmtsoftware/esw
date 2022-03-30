/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.extensions

/**
 * This is an extension class containing convenience functions for handling use cases involving Map type.
 */
object MapExt {

  implicit class MapOps[K, V](private val map: Map[K, V]) extends AnyVal {
    def addKeyIfNotExist(key: K, defaultValue: V): Map[K, V] =
      map.updatedWith(key)(_.fold(Some(defaultValue))(Some(_)))

    def addKeysIfNotExist(keys: List[K], defaultValue: V): Map[K, V] =
      keys.flatMap(addKeyIfNotExist(_, defaultValue)).toMap
  }

}
