package io.estatico.bson

import io.estatico.bson.macros.BsonCodecMacros
import scala.annotation.StaticAnnotation

class DeriveBson extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro BsonCodecMacros.deriveBsonAnnotation
}
