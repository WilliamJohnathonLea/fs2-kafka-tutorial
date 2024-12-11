package com.xebia.kafka

import cats.effect.kernel.Resource
import cats.effect.IO

import dev.profunktor.redis4cats.RedisCommands
import fs2._
import org.http4s.client.Client
import org.http4s.EntityDecoder
import org.http4s.Request
import org.http4s.Uri

import com.xebia.enrichment.UriProvider

object Components {

  def retrieveStatePipe[P](implicit
    redis: Resource[IO, RedisCommands[IO, String, String]]
  ): Pipe[IO, Packet[P], (FeedState, Packet[P])] =
    _.evalMap { packet =>
      val key = s"${packet.sport}:${packet.feedType}:${packet.feedId}:state"

      redis.use { redis =>
        val default = FeedState.empty
        for {
          exists <- redis.exists(key)
          state <- if (!exists)
                     IO.pure(default)
                   else
                     redis
                       .hGetAll(key)
                       .map(hash => FeedState.fromRedisHash(hash).getOrElse(default))
        } yield state -> packet
      }
    }

  def dedupPipe[P]: Pipe[IO, (FeedState, Packet[P]), (FeedState, Packet[P])] =
    _.evalMapFilter { case (state, packet) =>
      IO.pure {
        Option.when(packet.seqNum > state.lastSeqNum) {
          state.copy(lastSeqNum = packet.seqNum) -> packet
        }
      }
    }

  def enrichPipe[P](implicit
    client: Resource[IO, Client[IO]],
    provider: UriProvider,
    dec: EntityDecoder[IO, EnrichmentData]
  ): Pipe[IO, (FeedState, Packet[P]), (FeedState, Packet[P])] =
    _.evalMap { case (state, p) =>
      val uri = provider.provide(p.sport, p.feedId.toString())
      val req = Request[IO](uri = Uri.unsafeFromString(uri))
      client
        .use(_.expectOption[EnrichmentData](req))
        .recover { case _ => None }
        .map { data =>
          state.copy(enrichmentData = data) -> p
        }
    }

  def transformPipe[PIn, POut](
    fn: (FeedState, Packet[PIn]) => IO[(FeedState, Packet[POut])]
  ): Pipe[IO, (FeedState, Packet[PIn]), (FeedState, Packet[POut])] =
    _.evalMap { case (state, pIn) => fn(state, pIn) }

  def saveStatePipe[P](implicit
    redis: Resource[IO, RedisCommands[IO, String, String]]
  ): Pipe[IO, (FeedState, Packet[P]), (FeedState, Packet[P])] =
    _.evalMap { case (state, packet) =>
      val key = s"${packet.sport}:${packet.feedType}:${packet.feedId}:state"
      redis.use { redis =>
        redis.hSet(key, FeedState.toRedisHash(state)).as(state -> packet)
      }
    }

}
