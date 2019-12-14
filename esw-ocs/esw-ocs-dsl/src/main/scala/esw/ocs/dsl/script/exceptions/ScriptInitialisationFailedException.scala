package esw.ocs.dsl.script.exceptions

class ScriptInitialisationFailedException(msg: String) extends RuntimeException(s"Script initialization failed with : $msg")
