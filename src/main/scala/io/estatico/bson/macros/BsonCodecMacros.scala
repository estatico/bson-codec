package io.estatico.bson.macros

import io.estatico.bson.codecs.BsonCodec
import org.bson.BsonDocument
import scala.reflect.macros.blackbox

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

  def deriveBsonAnnotation(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {

    import c.universe._

    val BsonCodecClass = typeOf[BsonCodec[_]].typeSymbol.asType
    val BsonCodecCompanion = BsonCodecClass.companion
    val BsonDocumentClass = typeOf[BsonDocument].typeSymbol

    def mkInstance(clsDef: ClassDef): Tree = {
      val typeName = clsDef.name
      val instName = TermName("bsonCodec" + typeName.decodedName)
      if (clsDef.tparams.isEmpty) {
        q"""
          implicit val $instName: $BsonCodecCompanion.Aux[$typeName, $BsonDocumentClass] =
            $BsonCodecCompanion.deriveDocument[$typeName]
        """
      } else {
        val tparams = clsDef.tparams
        val tparamNames = tparams.map(_.name)
        def mkImplicitParams(typeSymbol: TypeSymbol) = tparamNames.map { tparamName =>
          val paramName = c.freshName(tparamName.toTermName)
          val paramType = tq"$typeSymbol[$tparamName]"
          q"$paramName: $paramType"
        }
        val params = mkImplicitParams(BsonCodecClass)
        val fullType = tq"$typeName[..$tparamNames]"
        q"""
          implicit def $instName[..$tparams](
            implicit ..$params
          ): $BsonCodecCompanion.Aux[$fullType, $BsonDocumentClass] =
           $BsonCodecCompanion.deriveDocument[$fullType]
        """
      }
    }

    annottees match {
      case List(clsDef: ClassDef) =>
        q"""
          $clsDef
          object ${clsDef.name.toTermName} { ${mkInstance(clsDef)} }
        """

      case List(
        clsDef: ClassDef,
        q"object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }"
      ) =>
        q"""
          $clsDef
          object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf =>
            ..$objDefs
            ${mkInstance(clsDef)}
          }
        """

       case _ => c.abort(c.enclosingPosition, s"Only case classes are supported.")
    }
  }
}
