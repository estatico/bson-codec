package io.estatico.bson.test

import org.bson._
import org.scalatest.{FlatSpec, Matchers}

trait BaseSpec extends FlatSpec with Matchers {

  protected def bDoc(kvs: (String, BsonValue)*): BsonDocument = {
    val o = new BsonDocument()
    kvs.foreach { case (k, v) => o.put(k, v) }
    o
  }

  protected def bArray(xs: BsonValue*): BsonArray = {
    val res = new BsonArray()
    xs.foreach { x => res.add(x) }
    res
  }

  protected def bNull: BsonValue = BsonNull.VALUE

  protected def bInt(n: Int) = new BsonInt32(n)

  protected def bLong(n: Int) = new BsonInt64(n)

  protected def bString(s: String) = new BsonString(s)
}
