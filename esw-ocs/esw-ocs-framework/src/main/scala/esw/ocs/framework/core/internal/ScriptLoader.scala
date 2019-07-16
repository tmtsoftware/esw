package esw.ocs.framework.core.internal

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigException}
import esw.ocs.framework.dsl.{CswServices, Script}
import esw.ocs.framework.exceptions.ScriptLoadingException._

import scala.util.control.NonFatal

class ScriptLoader(sequencerId: String, observingMode: String)(implicit actorSystem: ActorSystem[SpawnProtocol]) {
  // fixme: do not use config from actorSystem, load config only once in entire app, maybe in wiring
  private lazy val config: Config = actorSystem.settings.config

  // todo: can this be taken as dependency similar to seqId and obsMode and remove implicit actorSystem?
  private lazy val scriptClass: String = config.getString(s"scripts.$sequencerId.$observingMode.scriptClass")

  def load(cswServices: CswServices): Script = {
    try {
      val clazz = getClass.getClassLoader.loadClass(scriptClass)
      clazz.getConstructor(classOf[CswServices]).newInstance(cswServices).asInstanceOf[Script]
    } catch {
      case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(sequencerId, observingMode)
      case _: ClassCastException      => throw new InvalidScriptException(scriptClass)
      case _: ClassNotFoundException  => throw new ScriptNotFound(scriptClass)
      case NonFatal(e)                => throw e // fixme: no need to catch and rethrow, i.e. the default beh
    }
  }
}
