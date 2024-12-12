package com.xebia.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.util.Try

final case class FeedState(
  lastSeqNum: Int,
  enrichmentData: Option[EnrichmentData]
)

object FeedState {

  val empty: FeedState = FeedState(0, None)

  implicit val encoder: Encoder[FeedState] = deriveEncoder
  implicit val decoder: Decoder[FeedState] = deriveDecoder

  def fromRedisHash(hash: Map[String, String]): Option[FeedState] = {
    val data = hash.get("enrichmentData").flatMap(s => decode[EnrichmentData](s).toOption)
    Try(hash("lastSeqNum").toInt).toOption.map { seqNum =>
      FeedState(seqNum, data)
    }
  }

  def toRedisHash(s: FeedState): Map[String, String] =
    Map(
      "lastSeqNum" -> s.lastSeqNum.toString()
    ) ++ s.enrichmentData.map(d => "enrichmentData" -> d.asJson.noSpaces)

}
