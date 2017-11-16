package io.estatico.bson.ops

import io.estatico.bson.codecs.BsonCodec
import io.estatico.bson.ops.impl.IsNot
import org.bson.BsonValue
import scala.language.existentials

trait ToOps extends {}
  with ToBsonEncodeOps
  with ToBsonDecodeOps

final class BsonEncodeOps[A](val repr: A) extends AnyVal {
  def toBson(implicit ev: BsonCodec[A]): ev.Repr = ev.encode(repr)
}

trait ToBsonEncodeOps {
  implicit def toBsonEncodeOps[A](x: A): BsonEncodeOps[A] = new BsonEncodeOps[A](x)
}

/** Type class wrapper which enforces compile-time rules about decoding Bson values. */
final case class CanDecodeBson[A, B <: BsonValue](
  codec: BsonCodec.Aux[A, _ <: BsonValue]
) extends AnyVal

object CanDecodeBson {

  /**
   * We can decode a BsonValue to A so long as it has the exact type BsonValue and
   * there is some BsonCodec available for A. This is mostly a convenience so we don't
   * have to type check on Bson subtypes.
   */
  implicit def canDecodeBsonValue[A, B <: BsonValue](
    implicit
    ev: BsonCodec.Aux[A, _ <: BsonValue],
    b: B =:= BsonValue
  ): CanDecodeBson[A, B] = CanDecodeBson[A, B](ev)

  /**
   * We can decode a B to A if B is a subtype of BsonValue if there is a BsonCodec
   * available which encodes A to B. We add the constraint `B IsNot BsonValue` to
   * ensure that this doesn't conflict with our [[canDecodeBsonValue]] rule.
   */
  implicit def canDecodeBsonValueFromSubType[A, B <: BsonValue](
    implicit
    ev: BsonCodec.Aux[A, B],
    b: B IsNot BsonValue
  ): CanDecodeBson[A, B] = CanDecodeBson[A, B](ev)

  /**
   * We can decode a B to A if there is a BsonCodec available which encodes
   * A to BsonValue exactly as opposed to a subtype of BsonValue. For example, this rule
   * allows us to decode an Option[Int] from a BsonInt32 without having to up-cast it first
   * to BsonValue since the BsonCodec for Option encodes to a BsonValue (since it may
   * encode to BsonNull).
   */
  implicit def canDecodeSubTypeFromBsonValue[A, B <: BsonValue](
    implicit
    ev: BsonCodec.Aux[A, BsonValue],
    b: B IsNot BsonValue
  ): CanDecodeBson[A, B] = CanDecodeBson[A, B](ev)
}

final class BsonDecodeOps[B <: BsonValue](val repr: B) extends AnyVal {
  def as[A](implicit ev: CanDecodeBson[A, B]): BsonCodec.DecodeResult[A] = ev.codec.decode(repr)
}

trait ToBsonDecodeOps {
  implicit def toBsonDecodeOps[B <: BsonValue](x: B): BsonDecodeOps[B] = new BsonDecodeOps[B](x)
}
