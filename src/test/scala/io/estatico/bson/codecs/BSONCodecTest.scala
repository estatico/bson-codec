package io.estatico.bson.codecs

import io.estatico.bson.test.BaseSpec

class BSONCodecTest extends BaseSpec {

  behavior of classOf[BSONCodec[_]].getName

  it should "derive case classes" in {
    case class Foo(a: Int, b: String)
    implicit val codec: BSONObjectCodec[Foo] = BSONObjectCodec.derive[Foo]

    val encoded = BSONCodec[Foo].encode(Foo(12, "ha"))
    encoded shouldBe bsonObject("a" -> 12, "b" -> "ha")

    val decoded = BSONCodec[Foo].decode(bsonObject("a" -> 34, "b" -> "shoe"))
    decoded shouldBe Right(Foo(34, "shoe"))

    val failed = BSONCodec[Foo].decode(bsonObject("a" -> 12, "b" -> 34))
    failed.isLeft shouldBe true
  }

  it should "work for Map" in {
    val encoded1 = BSONCodec[Map[String, Int]].encode(Map("foo" -> 1, "bar" -> 2))
    encoded1 shouldBe bsonObject("foo" -> 1, "bar" -> 2)

    val encoded2 = BSONObjectCodec[Map[String, Int]].encode(Map("foo" -> 1, "bar" -> 2))
    encoded2 shouldBe bsonObject("foo" -> 1, "bar" -> 2)

    val decoded = BSONCodec[Map[String, Int]].decode(bsonObject("baz" -> 3, "quux" -> 7))
    decoded shouldBe Right(Map("baz" -> 3, "quux" -> 7))

    val failed = BSONCodec[Map[String, Int]].decode(bsonObject("baz" -> 8, "quux" -> "spam"))
    failed.isLeft shouldBe true
  }

  it should "work for List" in {
    val encoded1 = BSONCodec[List[Int]].encode(List(1, 2, 3))
    encoded1 shouldBe javaList(1, 2, 3)

    val encoded2 = BSONListCodec[List[Int]].encode(List(8, 3, 9))
    encoded2 shouldBe javaList(8, 3, 9)

    val decoded1 = BSONCodec[List[Int]].decode(javaList(9, 0, 3))
    decoded1 shouldBe Right(List(9, 0, 3))

    val decoded2 = BSONCodec[List[Int]].decode(bsonList(9, 0, 3))
    decoded2 shouldBe Right(List(9, 0, 3))

    val failed = BSONCodec[List[Int]].decode(bsonList(8, 6, "nope"))
    failed.isLeft shouldBe true
  }
}
