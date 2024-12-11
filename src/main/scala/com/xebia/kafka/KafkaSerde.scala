package com.xebia.kafka

import cats.effect.IO

import fs2.kafka._
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax._

object KafkaSerde {

  implicit def jsonValueDecoder[A: Decoder]: ValueDeserializer[IO, A] =
    Deserializer.instance[IO, A] { (_, _, bytes) =>
      IO.fromEither(decode(new String(bytes)))
    }

  implicit def jsonValueEncoder[A: Encoder]: ValueSerializer[IO, A] =
    Serializer.instance[IO, A] { (_, _, a) =>
      IO.pure(a.asJson.noSpaces.getBytes())
    }

}
