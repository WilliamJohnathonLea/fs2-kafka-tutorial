package com.xebia.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import java.util.UUID

final case class InboundPayload(
  action: String,
  team: UUID,
  player: Option[UUID]
)

object InboundPayload {

  implicit val encoder: Encoder[InboundPayload] = deriveEncoder
  implicit val decoder: Decoder[InboundPayload] = deriveDecoder

}
