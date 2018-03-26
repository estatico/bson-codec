package io.estatico.bson.codecs

final case class DecodeFailure(message: String, fields: Vector[String]) extends Exception {

  def insertField(field: String): DecodeFailure = copy(fields = field +: fields)

  def addField(field: String): DecodeFailure = copy(fields = fields :+ field)

  override def toString: String = s"DecodeFailure($message, $fields)"

  override def equals(o: scala.Any): Boolean = o match {
    case e: DecodeFailure => message == e.message && fields == e.fields
    case _ => false
  }
}

object DecodeFailure {
  def apply(message: String): DecodeFailure = DecodeFailure(message, Vector.empty)
}

final case class CodecFailure(message: String, fields: Vector[String]) extends Exception {

  def insertField(field: String): CodecFailure = copy(fields = field +: fields)

  def addField(field: String): CodecFailure = copy(fields = fields :+ field)

  override def toString: String = s"CodecFailure($message, $fields)"

  override def equals(o: scala.Any): Boolean = o match {
    case e: CodecFailure => message == e.message && fields == e.fields
    case _ => false
  }
}

object CodecFailure {
  def apply(message: String): CodecFailure = CodecFailure(message, Vector.empty)
}
