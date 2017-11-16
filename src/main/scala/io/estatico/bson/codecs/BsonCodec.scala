package io.estatico.bson.codecs

import org.bson._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox

/** Type class for encoding or decoding values to/from Bson. */
trait BsonCodec[A] {

  type Repr <: BsonValue

  /** Encode a value to its BSON representation. */
  def encode(a: A): Repr

  /** Decode a value from BSON, capturing failures as Left. */
  def decode(o: BsonValue): BsonCodec.DecodeResult[A]

  /** Update a BSONObject field with an encoded value; used for building BSONObjectCodec instances. */
  def put(o: BsonDocument, k: String, a: A): Unit = o.put(k, encode(a))

  /** Decode a value from a BSONObject field; used for building BSONObjectCodec instances. */
  def get(o: BsonDocument, k: String): BsonCodec.DecodeResult[A] = o.get(k) match {
    case null => Left(DecodeFailure(s"Missing field: $k", Vector(k)))
    case v => decode(v).left.map(_.insertField(k))
  }

  def imap[B](e: B => A, d: A => B): BsonCodec.Aux[B, Repr] = BsonCodec.instance[B, Repr](
    b => encode(e(b)),
    bson => decode(bson).map(d)
  )

  def iflatMap[B](e: B => A, d: A => BsonCodec.DecodeResult[B]): BsonCodec.Aux[B, Repr] = BsonCodec.instance[B, Repr](
    b => encode(e(b)),
    bson => decode(bson).flatMap(d)
  )
}

object BsonCodec extends BsonCodecInstances {

  type Aux[A, R <: BsonValue] = BsonCodec[A] { type Repr = R }

  type DecodeResult[A] = Either[DecodeFailure, A]

  def apply[A : BsonCodec](implicit codec: BsonCodec[A]): Aux[A, codec.Repr] = codec

  def instance[A, R <: BsonValue](
    e: A => R,
    d: BsonValue => DecodeResult[A]
  ): Aux[A, R] = new BsonCodec[A] {
    type Repr = R
    override def encode(a: A): Repr = e(a)
    override def decode(o: BsonValue): DecodeResult[A] = d(o)
  }

  /**
   * Derive an instance of BSONCodec[A] using runtime type checking.
   * This should only be used for non-object and non-container types like String, Int, etc.
   */
  def deriveValue[A <: BsonValue](implicit ct: ClassTag[A]): Aux[A, A] = new BsonCodec[A] {

    type Repr = A

    override def encode(a: A): A = a

    override def decode(o: BsonValue): DecodeResult[A] = o match {
      case ct(a) => Right(a)
      case _ => Left(DecodeFailure(s"Expected ${ct.runtimeClass} but got: ${o.getClass}"))
    }
  }

  def deriveDocument[A]: Aux[A, BsonDocument] = macro BsonCodecMacros.deriveDocument[A]

  /** Build a BsonArray-based BsonCodec for L[A] to/from iterators. */
  def fromIterator[L[_], A](
    toIterator: L[A] => Iterator[A],
    fromIterator: Iterator[A] => L[A]
  )(implicit codec: BsonCodec[A]): Aux[L[A], BsonArray] = apply[BsonArray].iflatMap(
    xs => {
      val res = new BsonArray()
      toIterator(xs).foreach { x => res.add(codec.encode(x)) }
      res
    },
    bson => {
      try {
        Right(fromIterator(bson.iterator.asScala.zipWithIndex.map { case (x, i) =>
          codec.decode(x).fold(
            e => throw e.addField(i.toString),
            identity
          )
        }))
      } catch {
        case e: DecodeFailure => Left(e)
      }
    }
  )
}

trait BsonCodecInstances {

  import BsonCodec.{apply, Aux, DecodeResult, deriveValue, fromIterator}

  implicit val bsonBsonInt32: Aux[BsonInt32, BsonInt32] = deriveValue
  implicit val bsonBsonInt64: Aux[BsonInt64, BsonInt64] = deriveValue
  implicit val bsonBsonString: Aux[BsonString, BsonString] = deriveValue
  implicit val bsonBsonArray: Aux[BsonArray, BsonArray] = deriveValue
  implicit val bsonBsonDocument: Aux[BsonDocument, BsonDocument] = deriveValue

  implicit val bsonInt: Aux[Int, BsonInt32] = apply[BsonInt32].imap(new BsonInt32(_), _.getValue)
  implicit val bsonLong: Aux[Long, BsonInt64] = apply[BsonInt64].imap(new BsonInt64(_), _.getValue)
  implicit val bsonString: Aux[String, BsonString] = apply[BsonString].imap(new BsonString(_), _.getValue)

  implicit def bsonList[A : BsonCodec]: Aux[List[A], BsonArray] =
    fromIterator(_.iterator, _.toList)

  implicit def bsonVector[A : BsonCodec]: Aux[Vector[A], BsonArray] =
    fromIterator(_.iterator, _.toVector)

  implicit def bsonSet[A : BsonCodec]: Aux[Set[A], BsonArray] =
    fromIterator(_.iterator, _.toSet)

  implicit def bsonMap[A](implicit codec: BsonCodec[A]): Aux[Map[String, A], BsonDocument] =
    apply[BsonDocument].iflatMap(
      m => {
        val res = new BsonDocument()
        m.foreach { case (k, v) => codec.put(res, k, v) }
        res
      },
      o =>
        try {
          Right(o.keySet.iterator.asScala.map { k =>
            val v = codec.get(o, k).fold(
              e => throw e.addField(k),
              identity
            )
            (k, v)
          }.toMap)
        } catch {
          case e: DecodeFailure => Left(e)
        }
    )

  // TODO: I have a feeling that this doesn't get the field path right when building a DecodeFailure
  implicit def bsonOption[A](implicit codec: BsonCodec[A]): Aux[Option[A], BsonValue] = new BsonCodec[Option[A]] {

    type Repr = BsonValue

    override def encode(a: Option[A]): BsonValue = a.fold(BsonNull.VALUE: BsonValue)(a => codec.encode(a))

    override def decode(o: BsonValue): DecodeResult[Option[A]] = o match {
      case null => Right(None)
      case BsonNull.VALUE => Right(None)
      case _ => codec.decode(o).right.map(Some(_))
    }

    override def put(o: BsonDocument, k: String, a: Option[A]): Unit =
      a.foreach { x => o.put(k, codec.encode(x)) }

    override def get(o: BsonDocument, k: String): DecodeResult[Option[A]] =
      decode(o.get(k))
  }
}

object BsonCodecMacros {

  def deriveDocument[A : c.WeakTypeTag](c: blackbox.Context): c.universe.Tree = {

    import c.universe._

    val A = weakTypeOf[A]

    if (!A.typeSymbol.isClass || !A.typeSymbol.asClass.isCaseClass) {
      c.abort(c.enclosingPosition, s"Can only derive a BsonDocument codec for case classes")
    }

    val fields = A.decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => (m.name, m.name.decodedName.toString, m.returnType)
    }.toVector

    val BsonCodecClass = typeOf[BsonCodec[_]].typeSymbol
    val BsonCodecCompanion = BsonCodecClass.companion
    val BsonDocumentClass = typeOf[BsonDocument].typeSymbol

    val encodeFields = fields.map { case (name, nameStr, typ) =>
      q"$BsonCodecCompanion[$typ].put(res, $nameStr, a.$name)"
    }

    val decodeFields = fields.map { case (name, nameStr, typ) =>
      fq"$name <- $BsonCodecCompanion[$typ].get(o, $nameStr).right"
    }

    q"""
      $BsonCodecCompanion[$BsonDocumentClass].iflatMap(
        a => {
          val res = new $BsonDocumentClass()
          ..$encodeFields
          res
        },
        o => for (..$decodeFields) yield new $A(..${fields.map(_._1)})
      ): $BsonCodecCompanion.Aux[$A, $BsonDocumentClass]
    """
  }
}
