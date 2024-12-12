package com.xebia.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import java.util.UUID

final case class EnrichmentData(
  players: Map[UUID, String],
  teams: Map[UUID, String]
)

object EnrichmentData {

  implicit val encoder: Encoder[EnrichmentData] = deriveEncoder
  implicit val decoder: Decoder[EnrichmentData] = deriveDecoder

}
