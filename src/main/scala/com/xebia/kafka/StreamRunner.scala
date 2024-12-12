package com.xebia.kafka

import cats.effect.kernel.Resource
import cats.effect.IO

import dev.profunktor.redis4cats._
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats._
import fs2.kafka._
import fs2.Stream
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import com.xebia.config.AppConfig
import com.xebia.enrichment.LocalUriProvider
import com.xebia.enrichment.UriProvider
import com.xebia.kafka.Components._
import com.xebia.kafka.KafkaSerde._
import com.xebia.model.FeedState
import com.xebia.model.Packet

import java.util.UUID

import scala.concurrent.duration._

class StreamRunner(conf: AppConfig) {

  implicit private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val redisCodec: RedisCodec[String, String] =
    RedisCodec.Utf8

  implicit private val redisComms: Resource[IO, RedisCommands[IO, String, String]] =
    RedisClient[IO]
      .from(conf.redisUri)
      .flatMap(Redis[IO].fromClient(_, redisCodec))

  implicit private val httpClient: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  implicit private val uriProvider: UriProvider =
    new LocalUriProvider

  def run[In: Decoder, Out: Encoder](
    transformFn: (FeedState, Packet[In]) => IO[(FeedState, Packet[Out])]
  ): IO[Unit] =
    KafkaProducer
      .stream(producerSettings[UUID, Packet[Out]])
      .flatMap { producer =>
        KafkaConsumer
          .stream(consumerSettings[Option[UUID], Packet[In]])
          .subscribeTo(conf.inboundTopic)
          .records
          .flatMap { record =>
            processPipe(record.record.value, transformFn)
              .map { case (_, packet) =>
                ProducerRecords.one(
                  ProducerRecord(
                    conf.outboundTopic,
                    packet.feedId,
                    packet
                  )
                )
              }
              .through(KafkaProducer.pipe(producer))
              .as(record.offset)
          }
          .through(commitBatchWithin(20, 15.seconds))
      }
      .compile
      .drain

  private def consumerSettings[K, V](implicit
    keyDeserializer: Resource[IO, KeyDeserializer[IO, K]],
    valueDeserializer: Resource[IO, ValueDeserializer[IO, V]]
  ): ConsumerSettings[IO, K, V] =
    ConsumerSettings[IO, K, V]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(conf.kafkaBootstrapServers)
      .withGroupId("test")

  private def producerSettings[K, V](implicit
    keySerializer: Resource[IO, KeySerializer[IO, K]],
    valueSerializer: Resource[IO, ValueSerializer[IO, V]]
  ): ProducerSettings[IO, K, V] =
    ProducerSettings[IO, K, V]
      .withBootstrapServers(conf.kafkaBootstrapServers)

  private def processPipe[PIn, POut](
    packetIn: Packet[PIn],
    fn: (FeedState, Packet[PIn]) => IO[(FeedState, Packet[POut])]
  ): Stream[IO, (FeedState, Packet[POut])] =
    Stream[IO, Packet[PIn]](packetIn)
      .through(retrieveStatePipe)
      .through(dedupPipe)
      .through(enrichPipe)
      .through(transformPipe(fn))
      .through(saveStatePipe)

}
