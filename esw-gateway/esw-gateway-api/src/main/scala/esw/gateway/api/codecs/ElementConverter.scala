package esw.gateway.api.codecs

import io.bullet.borer.Dom
import io.bullet.borer.Dom.Element

object ElementConverter {

  def toAny(element: Element): Any =
    element match {
      case Dom.NullElem             => null
      case Dom.BooleanElem(value)   => value
      case Dom.IntElem(value)       => value
      case Dom.LongElem(value)      => value
      case Dom.FloatElem(value)     => value
      case Dom.DoubleElem(value)    => value
      case Dom.StringElem(value)    => value
      case Dom.ByteArrayElem(value) => value
      case elem: Dom.ArrayElem      => toSeq(elem)
      case elem: Dom.MapElem        => toMap(elem)
      case elem                     => throw new RuntimeException(s"can not extract value from element=$elem")
    }

  def toMap(input: Dom.MapElem): Map[String, Any] =
    input.toMap.collect {
      case (Dom.StringElem(k), v) if v != Dom.NullElem => k -> toAny(v)
    }

  def toSeq(input: Dom.ArrayElem): Seq[Any] =
    input.elements.collect {
      case x if x != Dom.NullElem => toAny(x)
    }

  def fromAny(any: Any): Element =
    any match {
      case null           => Dom.NullElem
      case x: Boolean     => Dom.BooleanElem(x)
      case x: Int         => Dom.IntElem(x)
      case x: Long        => Dom.LongElem(x)
      case x: Float       => Dom.FloatElem(x)
      case x: Double      => Dom.DoubleElem(x)
      case x: String      => Dom.StringElem(x)
      case x: Array[Byte] => Dom.ByteArrayElem(x)
      case x: Seq[_]      => fromSeq(x)
      case x: Map[_, _]   => fromMap(x)
      case x              => throw new RuntimeException(s"can not create an element from value=$x")
    }

  def fromMap(input: Map[_, Any]): Dom.MapElem =
    Dom.MapElem.Unsized {
      input.collect {
        case (k, v) if v != null => fromAny(k) -> fromAny(v)
      }: Map[Element, Element]
    }

  def fromSeq(input: Seq[Any]): Dom.ArrayElem =
    Dom.ArrayElem.Unsized {
      input.collect {
        case x if x != null => fromAny(x)
      }.toVector
    }

}
