# bson-codec

BSON codec type classes and derivation for Scala

## Usage

```scala
scala> import io.estatico.bson._

scala> case class Person(name: String, age: Int)

scala> implicit val bsonPerson = BSONObjectCodec.derive[Person]

scala> val me = Person("Cary", 29)
me: Person = Person(Cary,29)

scala> val bson = me.toBSONObject
bson: org.bson.BSONObject = { "name" : "Cary" , "age" : 29}

scala> val decoded = bson.as[Person]
decoded: Either[Throwable,Person] = Right(Person(Cary,29))

scala> bson.put("age", "a million")
res0: Object = 29

scala> bson
res1: org.bson.BSONObject = { "name" : "Cary" , "age" : "a million"}

scala> bson.as[Person]
res2: Either[Throwable,Person] = Left(java.lang.IllegalArgumentException: Expected int but got: class java.lang.String)
```
