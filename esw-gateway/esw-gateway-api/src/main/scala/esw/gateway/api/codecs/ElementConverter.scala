package esw.gateway.api.codecs

import io.bullet.borer.Dom
import io.bullet.borer.Dom.Element

object ElementConverter {

  def toAny(element: Element): Any = element match {
    case Dom.NullElem             => null
    case Dom.BooleanElem(value)   => value
    case Dom.IntElem(value)       => value
    case Dom.LongElem(value)      => value
    case Dom.FloatElem(value)     => value
    case Dom.DoubleElem(value)    => value
    case Dom.StringElem(value)    => value
    case Dom.ByteArrayElem(value) => value
    case elem: Dom.ArrayElem      => elem.elements.map(toAny).filter(_ != null)
    case elem: Dom.MapElem =>
      elem.toMap
        .map { case (k, v) => toAny(k) -> toAny(v) }
        .filter(_._2 != null)
    case elem => throw new RuntimeException(s"can not extract value from element=$elem")
  }

  def fromAny(any: Any): Element = any match {
    case null           => Dom.NullElem
    case x: Boolean     => Dom.BooleanElem(x)
    case x: Int         => Dom.IntElem(x)
    case x: Long        => Dom.LongElem(x)
    case x: Float       => Dom.FloatElem(x)
    case x: Double      => Dom.DoubleElem(x)
    case x: String      => Dom.StringElem(x)
    case x: Array[Byte] => Dom.ByteArrayElem(x)
    case x: Seq[_]      => Dom.ArrayElem.Unsized(x.map(fromAny).filter(_ != null): _*)
    case x: Map[_, _] =>
      Dom.MapElem.Unsized(
        x.map { case (k, v) => fromAny(k) -> fromAny(v) }
          .filter(_._2 != null): Map[Element, Element]
      )
    case x => throw new RuntimeException(s"can not create an element from value =$x")
  }

}
