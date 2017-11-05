package io.estatico.bson.codecs

import java.util
import org.bson.{BasicBSONObject, BSONObject}
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import scala.util.control.NonFatal

/**
 * Base type class for encoding or decoding values to/from BSON.
 *
 * Notice that the `encode` method returns `Any` and the `decode` method
 * takes `Any`. This is because the BSONObject implementation stores its elements
 * dynamically as Java objects. As a result, usually, you won't want to use the `encode` and
 * `decode` methods directly. Instead, see the [[BSONObjectCodec]] and its
 * `encodeObject` and `decodeObject` methods.
 */
trait BSONCodec[A] {

  /** Encode a value to its BSON representation. */
  def encode(a: A): Any

  /** Decode a value from BSON, capturing failures as Left. */
  def decode(o: Any): Either[Throwable, A]

  /** Update a BSONObject field with an encoded value; used for building BSONObjectCodec instances. */
  def put(o: BSONObject, k: String, a: A): Unit

  /** Decode a value from a BSONObject field; used for building BSONObjectCodec instances. */
  def get(o: BSONObject, k: String): Either[Throwable, A]
}

/** Provides default implementations for `put` and `get`. */
trait DefaultBSONCodec[A] extends BSONCodec[A] {

  override def put(o: BSONObject, k: String, a: A): Unit = o.put(k, encode(a))

  override def get(o: BSONObject, k: String): Either[Throwable, A] = o.get(k) match {
    case null => Left(new NoSuchElementException(k))
    case v => decode(v)
  }
}

object BSONCodec extends BSONCodecInstances {

  def apply[A : BSONCodec]: BSONCodec[A] = implicitly

  def instance[A](e: A => Any, d: Any => Either[Throwable, A]): BSONCodec[A] = new DefaultBSONCodec[A] {
    override def encode(a: A): Any = e(a)
    override def decode(o: Any): Either[Throwable, A] = d(o)
  }

  /**
   * Derive an instance of BSONCodec[A] using runtime type checking.
   * This should only be used for non-object and non-container types like String, Int, etc.
   */
  def unsafeDerive[A](implicit ct: ClassTag[A]): BSONCodec[A] = new DefaultBSONCodec[A] {
    override def encode(a: A): Any = a
    override def decode(o: Any): Either[Throwable, A] = o match {
      case ct(a) => Right(a)
      case _ => Left(new IllegalArgumentException(s"Expected ${ct.runtimeClass} but got: ${o.getClass}"))
    }
  }
}

trait BSONCodecInstances {

  implicit val bsonInt: BSONCodec[Int] = BSONCodec.unsafeDerive[Int]
  implicit val bsonString: BSONCodec[String] = BSONCodec.unsafeDerive[String]

  implicit def bsonList[A : BSONCodec]: BSONListCodec[List[A]] =
    BSONListCodec.fromIterator(BSONListCodec.noSizeHint, _.iterator, _.toList)

  implicit def bsonVector[A : BSONCodec]: BSONListCodec[Vector[A]] =
    BSONListCodec.fromIterator(_.length, _.iterator, _.toVector)

  implicit def bsonSet[A : BSONCodec]: BSONListCodec[Set[A]] =
    BSONListCodec.fromIterator(_.size, _.iterator, _.toSet)

  implicit def bsonMap[A](implicit codec: BSONCodec[A]): BSONObjectCodec[Map[String, A]] =
    BSONObjectCodec.instance(
      m => {
        val res = new BasicBSONObject(m.size)
        m.foreach { case (k, v) => codec.put(res, k, v) }
        res
      },
      o =>
        try {
          Right(o.keySet.iterator.asScala.map(k =>
            (k, codec.get(o, k).fold(throw _, identity))
          ).toMap)
        } catch {
          case NonFatal(e) => Left(e)
        }
    )

  implicit def bsonOption[A](implicit codec: BSONCodec[A]): BSONCodec[Option[A]] = new BSONCodec[Option[A]] {

    override def encode(a: Option[A]): Any = a.fold(null: Any)(codec.encode)

    override def decode(o: Any): Either[Throwable, Option[A]] = o match {
      case null => Right(None)
      case _ => codec.decode(o).right.map(Some(_))
    }

    override def put(o: BSONObject, k: String, a: Option[A]): Unit = a.foreach { o.put(k, _) }

    override def get(o: BSONObject, k: String): Either[Throwable, Option[A]] = decode(o.get(k))
  }
}

/** Type class for encoding/decoding BSON lists. */
trait BSONListCodec[A] extends DefaultBSONCodec[A] {
  def encodeList(a: A): util.List[_]
  def decodeList(xs: util.List[_]): Either[Throwable, A]

  override def encode(a: A): Any = encodeList(a)

  override def decode(o: Any): Either[Throwable, A] = o match {
    case xs: util.List[Any] @unchecked => decodeList(xs)
    case _ => Left(new IllegalArgumentException("Expected java.util.List, got: " + o.getClass))
  }
}

object BSONListCodec {

  def apply[A : BSONListCodec]: BSONListCodec[A] = implicitly

  def instance[A](e: A => util.List[_], d: util.List[_] => Either[Throwable, A]): BSONListCodec[A] = new BSONListCodec[A] {
    override def encodeList(a: A): util.List[_] = e(a)
    override def decodeList(xs: util.List[_]): Either[Throwable, A] = d(xs)
  }

  /**
   * Build a BSONListCodec for L[A] to/from iterators.
   * Types like scala.List have expensive .size calls, so you can provide
   * `noSizeHint` to avoid iterating the List multiple times.
   */
  def fromIterator[L[_], A](
    sizeHint: L[A] => Int,
    toIterator: L[A] => Iterator[A],
    fromIterator: Iterator[A] => L[A]
  )(implicit codec: BSONCodec[A]): BSONListCodec[L[A]] = instance[L[A]](
    xs => {
      val res = new util.ArrayList[Any](sizeHint(xs))
      toIterator(xs).foreach { x => res.add(codec.encode(x)) }
      res
    },
    xs => {
      try {
        Right(fromIterator(xs.iterator.asScala.map { x =>
          codec.decode(x).fold(throw _, identity)
        }))
      } catch {
        case NonFatal(e) => Left(e)
      }
    }
  )

  val noSizeHint: Any => Int = _ => 0
}

/** Type class for encoding/decoding BSON objects. */
trait BSONObjectCodec[A] extends DefaultBSONCodec[A] {
  def encodeObject(a: A): BSONObject
  def decodeObject(o: BSONObject): Either[Throwable, A]

  override def encode(a: A): Any = encodeObject(a)

  override def decode(o: Any): Either[Throwable, A] = o match {
    case dbo: BSONObject => decodeObject(dbo)
    case _ => Left(new IllegalArgumentException("Expected DBObject type, got: " + o.getClass))
  }
}

object BSONObjectCodec {

  def apply[A : BSONObjectCodec]: BSONObjectCodec[A] = implicitly

  def instance[A](e: A => BSONObject, d: BSONObject => Either[Throwable, A]): BSONObjectCodec[A] = new BSONObjectCodec[A] {
    override def encodeObject(a: A): BSONObject = e(a)
    override def decodeObject(o: BSONObject): Either[Throwable, A] = d(o)
  }

  def derive[A]: BSONObjectCodec[A] = macro BSONCodecMacros.deriveObjectCodec[A]
}

object BSONCodecMacros {

  def deriveObjectCodec[A : c.WeakTypeTag](c: blackbox.Context): c.universe.Tree = {

    import c.universe._

    val A = weakTypeOf[A]

    if (!A.typeSymbol.isClass || !A.typeSymbol.asClass.isCaseClass) {
      c.abort(c.enclosingPosition, s"Can only derive ${classOf[BSONObjectCodec[_]].getName} for case classes")
    }

    val fields = A.decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => (m.name, m.name.decodedName.toString, m.returnType)
    }.toVector

    val BSONCodecCompanion = typeOf[BSONCodec[_]].typeSymbol.companion
    val BSONObjectCodecClass = typeOf[BSONObjectCodec[_]].typeSymbol
    val BSONObjectCodecCompanion = BSONObjectCodecClass.companion
    val BasicBSONObjectClass = typeOf[BasicBSONObject].typeSymbol

    val encodeFields = fields.map { case (name, nameStr, typ) =>
      q"$BSONCodecCompanion[$typ].put(res, $nameStr, a.$name)"
    }

    val decodeFields = fields.map { case (name, nameStr, typ) =>
      fq"$name <- $BSONCodecCompanion[$typ].get(o, $nameStr).right"
    }

    q"""
      $BSONObjectCodecCompanion.instance[$A](
        a => {
          val res = new $BasicBSONObjectClass(${fields.length})
          ..$encodeFields
          res
        },
        o => for (..$decodeFields) yield new $A(..${fields.map(_._1)})
      ): $BSONObjectCodecClass[$A]
    """
  }
}
