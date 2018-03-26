package io.estatico.bson.ops

import io.estatico.bson.DeriveBson
import io.estatico.bson.test.BaseSpec
import org.bson.BsonValue

class BsonOpsTest extends BaseSpec {

  behavior of "BsonOps"

  @DeriveBson case class Foo(a: Int, b: String)

  it should "encode bson documents" in {
    val encoded = Foo(12, "ha").toBson
    encoded shouldBe bDoc("a" -> bInt(12), "b" -> bString("ha"))
  }

  it should "decode bson documents" in {
    val decoded = bDoc("a" -> bInt(12), "b" -> bString("ha")).as[Foo]
    decoded shouldBe Right(Foo(12, "ha"))
  }

  it should "encode bson arrays" in {
    val encoded = List(Foo(12, "ha")).toBson
    encoded shouldBe bArray(bDoc("a" -> bInt(12), "b" -> bString("ha")))
  }

  it should "decode bson arrays" in {
    val decoded = bArray(bDoc("a" -> bInt(12), "b" -> bString("ha"))).as[List[Foo]]
    decoded shouldBe Right(List(Foo(12, "ha")))
  }

  it should "roundtrip Int" in {
    1.toBson.as[Int] shouldBe Right(1)
  }

  it should "roundtrip Option" in {
    Option(1).toBson.as[Option[Int]] shouldBe Right(Option(1))
  }

  it should "decode BsonValue to Option" in {
    (Option(1).toBson: BsonValue).as[Option[Int]] shouldBe Right(Option(1))
  }

  it should "decode BsonValue subtypes to Option" in {
    1.toBson.as[Option[Int]] shouldBe Right(Option(1))
  }

  it should "not compile .as for decoding non-document types to a document" in {
    assertDoesNotCompile("1.toBson.as[Foo]")
  }
}
