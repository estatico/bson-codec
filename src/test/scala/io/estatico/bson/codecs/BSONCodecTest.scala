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

  it should "work for Option" in {
    val encoded1 = BSONCodec[Option[Int]].encode(Some(1))
    encoded1 shouldBe 1

    val encoded2 = BSONCodec[Option[Int]].encode(None)
    encoded2 shouldBe null.asInstanceOf[Any]

    val decoded1 = BSONCodec[Option[Int]].decode(1)
    decoded1 shouldBe Right(Some(1))

    val decoded2 = BSONCodec[Option[Int]].decode(null)
    decoded2 shouldBe Right(None)

    val decoded3 = BSONCodec[Option[Int]].decode("foo")
    decoded3.isLeft shouldBe true
  }

  it should "omit null Option fields" in {
    case class Foo(a: Option[String], b: Option[Int])
    implicit val codec: BSONObjectCodec[Foo] = BSONObjectCodec.derive[Foo]

    val encoded1 = BSONCodec[Foo].encode(Foo(Some("abba"), Some(2)))
    encoded1 shouldBe bsonObject("a" -> "abba", "b" -> 2)

    val encoded2 = BSONCodec[Foo].encode(Foo(None, None))
    encoded2 shouldBe bsonObject()

    val encoded3 = BSONCodec[Foo].encode(Foo(None, Some(5)))
    encoded3 shouldBe bsonObject("b" -> 5)

    val encoded4 = BSONCodec[Foo].encode(Foo(Some("shoe"), None))
    encoded4 shouldBe bsonObject("a" -> "shoe")

    val decoded1 = BSONCodec[Foo].decode(bsonObject())
    decoded1 shouldBe Right(Foo(None, None))

    val decoded2 = BSONCodec[Foo].decode(bsonObject("a" -> "cat"))
    decoded2 shouldBe Right(Foo(Some("cat"), None))

    val decoded3 = BSONCodec[Foo].decode(bsonObject("b" -> 12))
    decoded3 shouldBe Right(Foo(None, Some(12)))

    val decoded4 = BSONCodec[Foo].decode(bsonObject("a" -> "baz", "b" -> 94))
    decoded4 shouldBe Right(Foo(Some("baz"), Some(94)))

    val decoded5 = BSONCodec[Foo].decode(bsonObject("a" -> 14, "b" -> 15))
    decoded5.isLeft shouldBe true
  }
}
