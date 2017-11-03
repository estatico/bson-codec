package io.estatico

//noinspection TypeAnnotation
package object bson extends bson.ops.ToOps {

  type BSONCodec[A] = bson.codecs.BSONCodec[A]
  val BSONCodec = bson.codecs.BSONCodec

  type BSONObjectCodec[A] = bson.codecs.BSONObjectCodec[A]
  val BSONObjectCodec = bson.codecs.BSONObjectCodec

  type BSONListCodec[A] = bson.codecs.BSONListCodec[A]
  val BSONListCodec = bson.codecs.BSONListCodec
}
