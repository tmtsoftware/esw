package esw.dsl.script.utils

import csw.params.core.generics.{KeyType, ParameterSetType}

// fixme : move this to appropriate place
object CommandUtils {

  implicit class RichCommand[T, S <: ParameterSetType[_]](command: S) {
    def getParam(keyName: String, keyType: KeyType[T]): T = command.get(keyName, keyType).get.head
  }

}
