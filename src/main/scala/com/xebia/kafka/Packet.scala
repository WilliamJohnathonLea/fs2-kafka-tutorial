package com.xebia.kafka

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import java.util.UUID

final case class Packet[P](
  seqNum: Int,
  sport: String,
  feedType: String,
  feedId: UUID,
  payload: P
)

object Packet {

  implicit def encoder[P: Encoder]: Encoder[Packet[P]] = deriveEncoder
  implicit def decoder[P: Decoder]: Decoder[Packet[P]] = deriveDecoder

}
