package com.xebia.kafka

import cats.effect.IO
import cats.effect.IOApp

import com.xebia.config.AppConfig
import com.xebia.model.FeedState
import com.xebia.model.InboundPayload
import com.xebia.model.OutboundPayload
import com.xebia.model.Packet

object Main extends IOApp.Simple {

  def tFn(
    s: FeedState,
    pIn: Packet[InboundPayload]
  ): IO[(FeedState, Packet[OutboundPayload])] = {
    val team = s.enrichmentData
      .flatMap(_.teams.get(pIn.payload.team))
      .getOrElse("unknown")
    val player = for {
      data <- s.enrichmentData
      pId <- pIn.payload.player
      pName <- data.players.get(pId)
    } yield pName

    val payload = OutboundPayload(
      action = pIn.payload.action,
      team = team,
      player = player
    )
    IO.pure {
      s -> Packet(
        pIn.seqNum,
        pIn.sport,
        pIn.feedType,
        pIn.feedId,
        payload
      )
    }
  }

  override def run: IO[Unit] = {
    val conf = AppConfig(
      kafkaBootstrapServers = "localhost:9092",
      inboundTopic = "inbound",
      outboundTopic = "outbound",
      redisUri = "redis://localhost:6379"
    )

    new StreamRunner(conf).run(tFn)
  }

}
