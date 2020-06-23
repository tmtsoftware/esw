package esw.sm.api.codecs

import csw.location.api.codec.LocationCodecs
import esw.ocs.api.codecs.OcsCodecs
import esw.sm.api.protocol.ShutdownSequencerResponse.UnloadScriptError
import esw.sm.api.protocol._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import msocket.api.codecs.BasicCodecs

object SequenceManagerCodecs extends SequenceManagerCodecs

trait SequenceManagerCodecs extends LocationCodecs with BasicCodecs with OcsCodecs {
  implicit lazy val configureResponseCodec: Codec[ConfigureResponse]                   = deriveAllCodecs
  implicit lazy val getRunningObsModesResponseCodec: Codec[GetRunningObsModesResponse] = deriveAllCodecs
  implicit lazy val cleanupResponseCodec: Codec[CleanupResponse]                       = deriveAllCodecs
  implicit lazy val startSequencerResponseCodec: Codec[StartSequencerResponse]         = deriveAllCodecs
  implicit lazy val shutdownSequencerResponseCodec: Codec[ShutdownSequencerResponse]   = deriveAllCodecs
  // todo: see if unloadScriptErrorCodec is required
  implicit lazy val unloadScriptErrorCodec: Codec[UnloadScriptError]                         = deriveCodec
  implicit lazy val shutdownAllSequencersResponseCodec: Codec[ShutdownAllSequencersResponse] = deriveAllCodecs
  implicit lazy val restartSequencerResponseCodec: Codec[RestartSequencerResponse]           = deriveAllCodecs
}
