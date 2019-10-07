package esw.gateway.api.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.alarm.models.Key.AlarmKey
import csw.location.api.codec.DoneCodec
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.Level
import csw.logging.models.codecs.LoggingCodecs
import csw.params.core.formats.ParamCodecs
import csw.params.events.EventKey
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.protocol._
import io.bullet.borer.Dom.{
  ArrayElem,
  BooleanElem,
  DoubleElem,
  Element,
  Float16Elem,
  FloatElem,
  IntElem,
  LongElem,
  MapElem,
  NumberStringElem,
  StringElem
}
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.{Codec, Decoder, Encoder}
import msocket.api.codecs.EitherCodecs

trait GatewayCodecs extends ParamCodecs with LocationCodecs with AlarmCodecs with EitherCodecs with DoneCodec {
  lazy val getEventErrorCodecValue: Codec[GetEventError] = {
    @silent implicit lazy val emptyEventKeysCodec: Codec[EmptyEventKeys.type] = singletonCodec(EmptyEventKeys)
    @silent implicit lazy val eventServerNotAvailableCodec: Codec[EventServerUnavailable.type] =
      singletonCodec(EventServerUnavailable)
    deriveCodec[GetEventError]
  }

  implicit def getEventErrorCodec[T <: GetEventError]: Codec[T] = getEventErrorCodecValue.asInstanceOf[Codec[T]]
  lazy val postRequestValue: Codec[PostRequest] = {
    @silent implicit lazy val submitCodec: Codec[Submit]                     = deriveCodec[Submit]
    @silent implicit lazy val onewayCodec: Codec[Oneway]                     = deriveCodec[Oneway]
    @silent implicit lazy val validateCodec: Codec[Validate]                 = deriveCodec[Validate]
    @silent implicit lazy val publishEventCodec: Codec[PublishEvent]         = deriveCodec[PublishEvent]
    @silent implicit lazy val getEventCodec: Codec[GetEvent]                 = deriveCodec[GetEvent]
    @silent implicit lazy val setAlarmSeverityCodec: Codec[SetAlarmSeverity] = deriveCodec[SetAlarmSeverity]
    @silent implicit lazy val levelCodec: Codec[Level]                       = LoggingCodecs.levelCodec
    @silent implicit val metadataDec: Decoder[Map[String, Any]] = {
      @silent implicit val anyDec: Decoder[Any] = Decoder.apply[Any](r => {
        if (r.hasString)
          r.readString
        else if (r.hasInt)
          r.readInt
        else if (r.hasLong)
          r.readLong
        else if (r.hasBoolean)
          r.readBoolean
        else if (r.hasDouble)
          r.readDouble
        else if (r.hasFloat)
          r.readFloat
        else null
      })
      Decoder.forMap
    }
    @silent implicit val metadataEnc: Encoder[Map[String, Any]] = {
      @silent implicit val anyEnc: Encoder[Any] = Encoder.apply[Any]((writer, value) => {
        value match {
          case x: Int    => writer.write(x)
          case x: Long   => writer.write(x)
          case x: Double => writer.write(x)
          case x: Float  => writer.write(x)
          case x: String => writer.write(x)
          case null      => writer.writeNull()
        }
      })
      Encoder.forMap
    }
    @silent implicit lazy val logCodec: Codec[Log] = deriveCodec[Log]
    deriveCodec[PostRequest]
  }

  implicit lazy val invalidComponentCodec: Codec[InvalidComponent] = deriveCodec[InvalidComponent]

  implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]

  implicit def postRequestCodec[T <: PostRequest]: Codec[T] = postRequestValue.asInstanceOf[Codec[T]]
  lazy val websocketRequestCodecValue: Codec[WebsocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal] = deriveCodec[QueryFinal]
    @silent implicit lazy val subscribeCodec: Codec[Subscribe]   = deriveCodec[Subscribe]
    @silent implicit lazy val subscribeWithPatternCodec: Codec[SubscribeWithPattern] =
      deriveCodec[SubscribeWithPattern]
    @silent implicit lazy val subscribeCurrentStateCodec: Codec[SubscribeCurrentState] =
      deriveCodec[SubscribeCurrentState]

    deriveCodec[WebsocketRequest]
  }

  def singletonErrorCodec[T <: SingletonError with Singleton](a: T): Codec[T] = Codec.bimap[String, T](_.msg, _ => a)

  private def getValue(element: Element): Any = {
    element match {
      case BooleanElem(value)      => value
      case StringElem(value)       => value
      case IntElem(value)          => value
      case LongElem(value)         => value
      case NumberStringElem(value) => value
      case DoubleElem(value)       => value
      case FloatElem(value)        => value
      case Float16Elem(value)      => value
      case elem: ArrayElem         => Array(elem.elements.map(getValue))
      case elem: MapElem           => getMap(elem)
      case _                       => null
    }
  }

  implicit def websocketRequestCodec[T <: WebsocketRequest]: Codec[T] =
    websocketRequestCodecValue.asInstanceOf[Codec[T]]

  private def getMap(element: MapElem): Map[String, Any] =
    element
      .asInstanceOf[MapElem]
      .toMap
      .map[String, Any] {
        case (k, v) => (k.asInstanceOf[StringElem].value, getValue(v))
      }
      .filter({
        case (_, v) => v != null
      })

  //Todo: move to csw
  implicit lazy val eventKeyCodec: Codec[EventKey] = deriveCodec[EventKey]
  implicit lazy val alarmKeyCodec: Codec[AlarmKey] = deriveCodec[AlarmKey]
}
