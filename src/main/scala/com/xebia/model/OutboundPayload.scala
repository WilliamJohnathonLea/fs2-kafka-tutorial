package com.xebia.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

final case class OutboundPayload(
  action: String,
  team: String,
  player: Option[String]
)

object OutboundPayload {

  implicit val encoder: Encoder[OutboundPayload] = deriveEncoder
  implicit val decoder: Decoder[OutboundPayload] = deriveDecoder

}
