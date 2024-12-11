package com.xebia.kafka

import cats.effect.IO
import cats.effect.IOApp

import com.xebia.config.AppConfig

object Main extends IOApp.Simple {

  override def run: IO[Unit] = {
    val conf = AppConfig(
      kafkaBootstrapServers = "localhost:9092",
      inboundTopic = "inbound",
      outboundTopic = "outbound",
      redisUri = "redis://localhost:6379"
    )

    val tFn: (FeedState, Packet[Int]) => IO[(FeedState, Packet[String])] =
      (s, pIn) =>
        IO.pure {
          s -> Packet(
            pIn.seqNum,
            pIn.sport,
            pIn.feedType,
            pIn.feedId,
            pIn.payload.toString()
          )
        }

    new StreamRunner(conf).run(tFn)
  }

}
