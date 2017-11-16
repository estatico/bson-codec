package io.estatico

package object bson extends bson.ops.ToOps {

  type BsonCodec[A] = bson.codecs.BsonCodec[A]
  val BsonCodec = bson.codecs.BsonCodec
}
