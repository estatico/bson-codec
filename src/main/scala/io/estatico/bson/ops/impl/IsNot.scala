package io.estatico.bson.ops.impl

// Adapted from shapeless' =:!=
trait IsNot[A, B]

object IsNot {
  implicit def neg[A, B]: A IsNot B = null
  implicit def negAmbiguous1[A]: A IsNot A = null
  implicit def negAmbiguous2[A]: A IsNot A = null
}
