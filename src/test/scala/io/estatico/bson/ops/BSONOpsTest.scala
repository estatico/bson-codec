package io.estatico.bson.ops

import io.estatico.bson.codecs.BSONObjectCodec
import io.estatico.bson.test.BaseSpec

class BSONOpsTest extends BaseSpec {

  behavior of "BSONOps"

  case class Foo(a: Int, b: String)
  implicit val codec = BSONObjectCodec.derive[Foo]

  it should "encode bson objects" in {
    val encoded = Foo(12, "ha").toBSONObject
    encoded shouldBe bsonObject("a" -> 12, "b" -> "ha")
  }

  it should "decode bson objects" in {
    val decoded1 = bsonObject("a" -> 12, "b" -> "ha").as[Foo]
    decoded1 shouldBe Right(Foo(12, "ha"))

    val decoded2 = bsonObject("a" -> 12, "b" -> "ha").decodeBSONObject[Foo]
    decoded2 shouldBe Right(Foo(12, "ha"))
  }

  it should "encode bson lists" in {
    val encoded = List(Foo(12, "ha")).toBSONList
    encoded shouldBe javaList(bsonObject("a" -> 12, "b" -> "ha"))
  }

  it should "decode java lists" in {
    val decoded = javaList(bsonObject("a" -> 12, "b" -> "ha")).decodeBSONList[List[Foo]]
    decoded shouldBe Right(List(Foo(12, "ha")))
  }

  it should "decode bson lists" in {
    val decoded1 = bsonList(bsonObject("a" -> 12, "b" -> "ha")).as[List[Foo]]
    decoded1 shouldBe Right(List(Foo(12, "ha")))

    val decoded2 = bsonList(bsonObject("a" -> 12, "b" -> "ha")).decodeBSONList[List[Foo]]
    decoded2 shouldBe Right(List(Foo(12, "ha")))
  }
}
