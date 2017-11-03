package io.estatico.bson.ops

import io.estatico.bson.codecs.{BSONCodec, BSONListCodec, BSONObjectCodec}
import java.util
import org.bson.BSONObject

trait ToOps extends {}
  with ToBSONObjectEncodeOps
  with ToBSONListEncodeOps
  with ToBSONObjectDecodeOps
  with ToBSONListDecodeOps

final class BSONObjectEncodeOps[A](val repr: A) extends AnyVal {
  def toBSONObject(implicit ev: BSONObjectCodec[A]): BSONObject = ev.encodeObject(repr)
}

trait ToBSONObjectEncodeOps {
  implicit def toBSONObjectEncodeOps[A](x: A): BSONObjectEncodeOps[A] = new BSONObjectEncodeOps[A](x)
}

final class BSONListEncodeOps[A](val repr: A) extends AnyVal {
  def toBSONList(implicit ev: BSONListCodec[A]): util.List[_] = ev.encodeList(repr)
}

trait ToBSONListEncodeOps {
  implicit def toBSONListEncodeOps[A](x: A): BSONListEncodeOps[A] = new BSONListEncodeOps[A](x)
}

final class BSONObjectDecodeOps(val repr: BSONObject) extends AnyVal {

  def as[A](implicit ev: BSONCodec[A]): Either[Throwable, A] = ev.decode(repr)

  def decodeBSONObject[A](implicit ev: BSONObjectCodec[A]): Either[Throwable, A] = ev.decodeObject(repr)
}

trait ToBSONObjectDecodeOps {
  implicit def toBSONObjectDecodeOps(x: BSONObject): BSONObjectDecodeOps = new BSONObjectDecodeOps(x)
}

final class BSONListDecodeOps(val repr: util.List[_]) extends AnyVal {
  def decodeBSONList[A](implicit ev: BSONListCodec[A]): Either[Throwable, A] = ev.decodeList(repr)
}

trait ToBSONListDecodeOps {
  implicit def toBSONListDecodeOps(x: util.List[_]): BSONListDecodeOps = new BSONListDecodeOps(x)
}
