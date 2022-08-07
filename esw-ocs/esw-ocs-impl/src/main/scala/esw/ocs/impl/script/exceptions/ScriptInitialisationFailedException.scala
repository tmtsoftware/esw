package esw.ocs.impl.script.exceptions

class ScriptInitialisationFailedException(msg: String) extends RuntimeException(s"Script initialization failed with : $msg")
