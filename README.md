# bson-codec

BSON codec type classes and derivation for Scala

[![Build Status](https://travis-ci.org/estatico/bson-codec.svg?branch=master)](https://travis-ci.org/estatico/bson-codec)
[![Maven Central](https://img.shields.io/maven-central/v/io.estatico/bson-codec_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.estatico/bson-codec_2.12)

## Usage

This library provides a `BsonCodec` type class for encoding and decoding BSON values.

```scala
scala> import io.estatico.bson._

scala> case class Person(name: String, age: Int)

scala> implicit val bsonPerson = BsonCodec.deriveDocument[Person]
bsonPerson: io.estatico.bson.BsonCodec.Aux[Person,org.bson.BsonDocument] = io.estatico.bson.codecs.BsonCodec$$anon$1@5ad87570

scala> val me = Person("Cary", 29)
me: Person = Person(Cary,29)

scala> val bson = me.toBson
bson: bsonPerson.Repr = { "name" : "Cary", "age" : 29 }

scala> val decoded = bson.as[Person]
decoded: io.estatico.bson.codecs.BsonCodec.DecodeResult[Person] = Right(Person(Cary,29))

scala> bson.put("age", "a million".toBson)
res0: org.bson.BsonValue = BsonInt32{value=29}

scala> bson
res1: bsonPerson.Repr = { "name" : "Cary", "age" : "a million" }

scala> bson.as[Person]
res2: io.estatico.bson.codecs.BsonCodec.DecodeResult[Person] = Left(DecodeFailure(Expected class org.bson.BsonInt32 but got: class org.bson.BsonString, Vector(age)))
```

`BsonCodec` is smart and _knows_ what type to encode to and decode from -

```scala
scala> "foo".toBson: org.bson.BsonString
res3: org.bson.BsonString = BsonString{value='foo'}

scala> 1.toBson: org.bson.BsonInt32
res4: org.bson.BsonInt32 = BsonInt32{value=1}

scala> 1L.toBson: org.bson.BsonInt64
res5: org.bson.BsonInt64 = BsonInt64{value=1}

scala> me.toBson: org.bson.BsonDocument
res6: org.bson.BsonDocument = { "name" : "Cary", "age" : 29 }

scala> List(1,2,3).toBson: org.bson.BsonArray
res7: org.bson.BsonArray = BsonArray{values=[BsonInt32{value=1}, BsonInt32{value=2}, BsonInt32{value=3}]}
```

As such, the compiler can help you by preventing you from attempting to decode certain
values which would be impossible -

```scala
scala> 1.toBson.as[Int]
res8: io.estatico.bson.codecs.BsonCodec.DecodeResult[Int] = Right(1)

scala> 1.toBson.as[String]
<console>:15: error: could not find implicit value for parameter ev: io.estatico.bson.ops.CanDecodeBson[String,io.estatico.bson.codecs.BsonCodec.bsonInt.Repr]
       1.toBson.as[String]
                  ^

scala> me.toBson.as[List[Int]]
<console>:17: error: could not find implicit value for parameter ev: io.estatico.bson.ops.CanDecodeBson[List[Int],bsonPerson.Repr]
       me.toBson.as[List[Int]]
                   ^
```
