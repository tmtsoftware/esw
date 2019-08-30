package esw.highlevel.dsl

import csw.params.core.generics.{KeyType, ParameterSetType}

object Util {
  implicit class RichCommand[T, S <: ParameterSetType[_]](command: S) {
    def getParam(keyName: String, keyType: KeyType[T]): T = command.get(keyName, keyType).get.head
  }
}
