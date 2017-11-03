package io.estatico.bson.test

import java.util
import org.bson.{BasicBSONObject, BSONObject}
import org.bson.types.BasicBSONList
import org.scalatest.{FlatSpec, Matchers}

trait BaseSpec extends FlatSpec with Matchers {

  protected def bsonObject(kvs: (String, Any)*): BSONObject = {
    val o = new BasicBSONObject(kvs.length)
    kvs.foreach { case (k, v) => o.put(k, v.asInstanceOf[AnyRef]) }
    o
  }

  protected def bsonList(xs: Any*): BasicBSONList = {
    val res = new BasicBSONList
    xs.foreach { x => res.add(x.asInstanceOf[AnyRef]) }
    res
  }

  protected def javaList(xs: Any*): util.List[Any] = {
    val res = new util.ArrayList[Any](xs.length)
    xs.foreach { x => res.add(x) }
    res
  }
}
