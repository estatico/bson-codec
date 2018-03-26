package io.estatico.bson.codecs

import io.estatico.bson.DeriveBson
import io.estatico.bson.test.BaseSpec

class BsonCodecTest extends BaseSpec {

  behavior of classOf[BsonCodec[_]].getName

  it should "derive case classes" in {
    @DeriveBson case class Foo(a: Int, b: String)

    val encoded = BsonCodec[Foo].encode(Foo(12, "ha"))
    encoded shouldBe bDoc("a" -> bInt(12), "b" -> bString("ha"))

    val decoded = BsonCodec[Foo].decode(bDoc("a" -> bInt(34), "b" -> bString("shoe")))
    decoded shouldBe Right(Foo(34, "shoe"))

    val failed = BsonCodec[Foo].decode(bDoc("a" -> bInt(12), "b" -> bInt(34)))
    failed.isLeft shouldBe true
  }

  it should "work for Map" in {
    val encoded1 = BsonCodec[Map[String, Int]].encode(Map("foo" -> 1, "bar" -> 2))
    encoded1 shouldBe bDoc("foo" -> bInt(1), "bar" -> bInt(2))

    val encoded2 = BsonCodec[Map[String, Int]].encode(Map("foo" -> 1, "bar" -> 2))
    encoded2 shouldBe bDoc("foo" -> bInt(1), "bar" -> bInt(2))

    val decoded = BsonCodec[Map[String, Int]].decode(bDoc("baz" -> bInt(3), "quux" -> bInt(7)))
    decoded shouldBe Right(Map("baz" -> 3, "quux" -> 7))

    val failed = BsonCodec[Map[String, Int]].decode(bDoc("baz" -> bInt(8), "quux" -> bString("spam")))
    failed.isLeft shouldBe true
  }

  it should "work for List" in {
    val encoded1 = BsonCodec[List[Int]].encode(List(1, 2, 3))
    encoded1 shouldBe bArray(bInt(1), bInt(2), bInt(3))

    val encoded2 = BsonCodec[List[Int]].encode(List(8, 3, 9))
    encoded2 shouldBe bArray(bInt(8), bInt(3), bInt(9))

    val decoded1 = BsonCodec[List[Int]].decode(bArray(bInt(9), bInt(0), bInt(3)))
    decoded1 shouldBe Right(List(9, 0, 3))

    val decoded2 = BsonCodec[List[Int]].decode(bArray(bInt(9), bInt(0), bInt(3)))
    decoded2 shouldBe Right(List(9, 0, 3))

    val failed = BsonCodec[List[Int]].decode(bArray(bInt(8), bInt(6), bString("nope")))
    failed.isLeft shouldBe true
  }

  it should "work for Option" in {
    val encoded1 = BsonCodec[Option[Int]].encode(Some(1))
    encoded1 shouldBe bInt(1)

    val encoded2 = BsonCodec[Option[Int]].encode(None)
    encoded2 shouldBe bNull

    val decoded1 = BsonCodec[Option[Int]].decode(bInt(1))
    decoded1 shouldBe Right(Some(1))

    val decoded2 = BsonCodec[Option[Int]].decode(null)
    decoded2 shouldBe Right(None)

    val decoded3 = BsonCodec[Option[Int]].decode(bNull)
    decoded3 shouldBe Right(None)

    val decoded4 = BsonCodec[Option[Int]].decode(bString("foo"))
    decoded4.isLeft shouldBe true
  }

  it should "omit null Option fields" in {
    @DeriveBson case class Foo(a: Option[String], b: Option[Int])

    val encoded1 = BsonCodec[Foo].encode(Foo(Some("abba"), Some(2)))
    encoded1 shouldBe bDoc("a" -> bString("abba"), "b" -> bInt(2))

    val encoded2 = BsonCodec[Foo].encode(Foo(None, None))
    encoded2 shouldBe bDoc()

    val encoded3 = BsonCodec[Foo].encode(Foo(None, Some(5)))
    encoded3 shouldBe bDoc("b" -> bInt(5))

    val encoded4 = BsonCodec[Foo].encode(Foo(Some("shoe"), None))
    encoded4 shouldBe bDoc("a" -> bString("shoe"))

    val decoded1 = BsonCodec[Foo].decode(bDoc())
    decoded1 shouldBe Right(Foo(None, None))

    val decoded2 = BsonCodec[Foo].decode(bDoc("a" -> bString("cat")))
    decoded2 shouldBe Right(Foo(Some("cat"), None))

    val decoded3 = BsonCodec[Foo].decode(bDoc("b" -> bInt(12)))
    decoded3 shouldBe Right(Foo(None, Some(12)))

    val decoded4 = BsonCodec[Foo].decode(bDoc("a" -> bString("baz"), "b" -> bInt(94)))
    decoded4 shouldBe Right(Foo(Some("baz"), Some(94)))

    val decoded5 = BsonCodec[Foo].decode(bDoc("a" -> bInt(14), "b" -> bInt(15)))
    decoded5.isLeft shouldBe true
  }

  it should "give path to decode failure for List" in {
    @DeriveBson case class Foo(bar: Bar)
    @DeriveBson case class Bar(baz: Baz)
    @DeriveBson case class Baz(quux: List[Int])

    val decoded = BsonCodec[Foo].decode(bDoc(
      "bar" -> bDoc(
        "baz" -> bDoc(
          "quux" -> bArray(
            bInt(1),
            bString("nope"),
            bInt(3)
          )
        )
      )
    ))

    decoded.left.get.fields shouldBe Vector("bar", "baz", "quux", "1")
  }

  it should "give path to decode failure for Option" in {
    @DeriveBson case class Foo(bar: Bar)
    @DeriveBson case class Bar(baz: Baz)
    @DeriveBson case class Baz(quux: Option[Int])
    val decoded = BsonCodec[Foo].decode(bDoc(
      "bar" -> bDoc(
        "baz" -> bDoc(
          "quux" -> bString("no")
        )
      )
    ))
    decoded.left.get.fields shouldBe Vector("bar", "baz", "quux")
  }

  it should "give path to decode failure for Map" in {
    @DeriveBson case class Foo(bar: Bar)
    @DeriveBson case class Bar(baz: Baz)
    @DeriveBson case class Baz(quux: Map[String, Int])

    val decoded = BsonCodec[Foo].decode(bDoc(
      "bar" -> bDoc(
        "baz" -> bDoc(
          "quux" -> bDoc(
            "spam" -> bInt(1),
            "eggs" -> bString("nope")
          )
        )
      )
    ))

    decoded.left.get.fields shouldBe Vector("bar", "baz", "quux", "eggs")
  }
}
