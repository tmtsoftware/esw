package esw.ocs.app

import io.bullet.borer.{Codec, Json}

object SerdeTest extends App {
  case class MyClass(value: Option[Int] = None)
  import io.bullet.borer.derivation.MapBasedCodecs._
//  import io.bullet.borer.NullOptions._
  implicit lazy val testCodec: Codec[MyClass] = deriveCodec

  //with values
  val encoded1 = Json.encode(MyClass(Some(3))).toUtf8String
  println(encoded1)
  val decoded1 = Json.decode("{\"value\": 3}".getBytes).to[MyClass].value
  println(decoded1)

  //without values
  val encoded2 = Json.encode(MyClass(None)).toUtf8String
  println(encoded2)
  val decoded2 = Json.decode("{}".getBytes).to[MyClass].value
  println(decoded2)
}

//setup with default codec
//{"Setup":{"source":"CSW","commandName":"command-1","maybeObsId":[],"paramSet":[]}},"status":{"Success":{}}
//{"Setup":{"source":"CSW","commandName":"command-1","maybeObsId":null,"paramSet":null}},"status":{"Success":{}}
//{"Setup":{"source":"CSW","commandName":"command-1"}},"status":{"Success":{}}
